package at.dms.kjc.spacetime;

import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.common.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.spacedynamic.Util;

import java.util.Hashtable;
import at.dms.util.SIRPrinter;
//import at.dms.compiler.*;

/**
 * This class returns the c code (a string) for a given raw tile
 */
public class TraceIRtoC extends ToC
{
    //for the first tile we encounter we are going to 
    //create a magic instruction that tells the number-gathering
    //stuff that everything is done snake booting, so if this is true
    //don't generate the magic instruction
    private static boolean gen_magc_done_boot = false;

    /** the name of the var that holds the dynamic message header */
    public static final String DYNMSGHEADER = "__DYNMSGHEADER__";
    /** a var name used to receive data from the dram on the compute proc 
     * over the gdn that is not needed but only generated because we have
     * cache-line sized transfers from drams.
     */ 
    public static final String DUMMY_VOLATILE = "__dummy_volatile";
    
    //the raw tile we are generating code for
    private ComputeNode tile;
    
    public TraceIRtoC(ComputeNode tile) 
    {
        super();
        this.tile = tile;
    }

    public TraceIRtoC() 
    {
        super();
        this.tile = null;
    }
    
    
    /** 
     * The entry point to create C code for a RawTile
     **/
    public void createCCode() 
    {
        //optimize the SIR code, if not disabled
        (new at.dms.kjc.sir.lowering.FinalUnitOptimize()).optimize(tile.getComputeCode());
        
        //write header 
        generateHeader();
        
        //generate the fields
        for (int i = 0; i < tile.getComputeCode().getFields().length; i++)
            tile.getComputeCode().getFields()[i].accept(this);
        
        p.print("\n");
        
        //print the pointers to the off chip buffers
//        if (!KjcOptions.magicdram)
            p.print(CommunicateAddrs.getFields(tile));

        
        
        //visit methods of tile, print the declaration first
        setDeclOnly(true);
        for (int i = 0; i < tile.getComputeCode().getMethods().length; i++)
            tile.getComputeCode().getMethods()[i].accept(this);

        //generate the methods
        setDeclOnly(false);
        JMethodDeclaration mainMethod=tile.getComputeCode().getMainFunction();
        for (int i = 0; i < tile.getComputeCode().getMethods().length; i++) {
            JMethodDeclaration method=tile.getComputeCode().getMethods()[i];
            if(method!=mainMethod) //Manually inline main method so inline asm labels don't repeat
                method.accept(this);
        }
        

//        if (!KjcOptions.magicdram) {
            //print the method that sends/recvs block addresses
            p.print(CommunicateAddrs.getFunction(tile));
            //print the method that sets up the rotation for the buffers
            p.print(CommunicateAddrs.getRotSetupFunct(tile));
//        }
        
        //generate the entry function for the simulator
        p.print("void begin(void) {\n");
        
        //for the first tile we encounter we are going to 
        //create a magic instruction that tells the number-gathering
        //stuff that everything is done snake booting, so if this is true
        if (!gen_magc_done_boot && KjcOptions.numbers > 0) {
            gen_magc_done_boot = true;
            p.print("  __asm__ volatile (\"magc $0, $0, 5\");\n");
        }
        //if we are using the magic network, 
        //use a magic instruction to initialize the magic fifos
        if (KjcOptions.magic_net)
            p.print("  __asm__ volatile (\"magc $0, $0, 1\");\n");

        //call the raw_init() function for the static network
        if (!KjcOptions.decoupled && !KjcOptions.magic_net) {
            p.print("  raw_init();\n");
            //print("  raw_init2();\n");
        }

//        if (!KjcOptions.magicdram) { 
            p.print("  " + CommunicateAddrs.functName + "();\n");
            p.print("  " + CommunicateAddrs.rotationSetupFunction + "();\n");
//        }
        //print(tile.getComputeCode().getMainFunction().getName() + "();\n");
        method = mainMethod;
        mainMethod.getBody().accept(this); //Inline Main method
        method = null;
        p.print("};\n");
    }
        
    
    private void generateHeader() 
    {
        p.print("#include <raw.h>\n");
        p.print("#include \"/home/bits6/mgordon/streams/include/raw_streaming_gdn.h\"\n");
        p.print("#include <stdlib.h>\n");
        p.print("#include <math.h>\n\n");
        
        p.print(RawSimulatorPrint.getHeader());
        
        //if we are number gathering and this is the sink, generate the dummy
        //vars for the assignment of the print expression.
        /*
          if (KjcOptions.numbers > 0 && NumberGathering.successful &&
          self == NumberGathering.sink.contents) {
          p.print("int dummyInt;\n");
          p.print("float dummyFloat;\n");
          }
        */
        
//      print the typedefs for the rotating buffers
//        if (!KjcOptions.magicdram) {
            p.print(CommunicateAddrs.getRotationTypes());
//        }
        
        if (/*KjcOptions.altcodegen && */ !KjcOptions.decoupled){
            p.print(getNetRegsDecls());
        }
        
        if (KjcOptions.decoupled) {
            p.print("float " + Util.CSTOFPVAR + ";\n");
            p.print("float " + Util.CSTIFPVAR + ";\n");
            p.print("int " + Util.CSTOINTVAR + ";\n");
            p.print("int " + Util.CSTIINTVAR + ";\n");
        }
        
        //print the declaration for the var used to receive 
        //words that are sent over from the dram but not used.  Due to 
        //cache-line multiple transfers.
        p.print("volatile int " + DUMMY_VOLATILE + ";\n");
                
        //if there are structures in the code, include
        //the structure definition header files
        //      if (SpaceTimeBackend.structures.length > 0) 
        // p.print("#include \"structs.h\"\n");

        //print the extern for the function to init the 
        p.print("void raw_init();\n");
        p.print("void raw_init2();\n");


        if (SpaceTimeBackend.FILTER_DEBUG_MODE) {
            p.print("void static_send_print(");
            p.print("int i) {\n");
            p.print("\tprint_int(i);\n");
            p.print("\tstatic_send(i);\n");
            p.print("}\n\n");
            p.print("void static_send_print_f(");
            p.print("float f) {\n");
            p.print("\tprint_float(f);\n");
            p.print("\tstatic_send(f);\n");
            p.print("}\n\n");
        }
    }

    public static String getNetRegsDecls() 
    {
        StringBuffer buf = new StringBuffer();
 
        buf.append("register float " + Util.CSTOFPVAR + " asm(\"$csto\");\n");
        buf.append("register float " + Util.CSTIFPVAR + " asm(\"$csti\");\n");
        buf.append("register int " + Util.CSTOINTVAR + " asm(\"$csto\");\n");
        buf.append("register int " + Util.CSTIINTVAR + " asm(\"$csti\");\n");
        buf.append("register float " + Util.CGNOFPVAR + " asm(\"$cgno\");\n");
        buf.append("register float " + Util.CGNIFPVAR + " asm(\"$cgni\");\n");
        buf.append("register int " + Util.CGNOINTVAR + " asm(\"$cgno\");\n");
        buf.append("register int " + Util.CGNIINTVAR + " asm(\"$cgni\");\n");
        buf.append("unsigned " + DYNMSGHEADER + ";\n");
        
        return buf.toString();
    }
    
    /**
     * prints a for statement
     */
    public void visitForStatement(JForStatement self,
                                  JStatement init,
                                  JExpression cond,
                                  JStatement incr,
                                  JStatement body) {
        p.print("for (");


        if (init != null) {
            init.accept(this);
            //the ; will print in a statement visitor
        }
        else //if null we must print the ; explicitly
            p.print(";");
        //we must also do this if we encounter a jemptystatement here
        if (init instanceof JEmptyStatement)
            p.print(";");

        p.print(" ");
        if (cond != null) {
            cond.accept(this);
        }
        //cond is an expression so print the ;
        p.print("; ");
        if (incr != null) {
            TraceIRtoC l2c = new TraceIRtoC(tile);
            incr.accept(l2c);
            // get String
            String str = l2c.getPrinter().getString();
            // leave off the trailing semicolon if there is one
            if (str.endsWith(";")) {
                p.print(str.substring(0, str.length()-1));
            } else { 
                p.print(str);
            }
        }

        p.print(") ");

        p.print("{");
        p.indent();
        body.accept(this);
        p.outdent();
        p.newLine();
        p.print("}");
    }
    
    /**
     * prints a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {
        if (expr instanceof JArrayInitializer) {
            declareInitializedArray(type, ident, expr);
        } else {
        
            printDecl (type, ident);
        
            if (expr != null && !(expr instanceof JNewArrayExpression)) {
                p.print ("\t= ");
                expr.accept (this);
            } else if (type.isOrdinal())
                p.print(" = 0");
            else if (type.isFloatingPoint())
                p.print(" = 0.0f");
            else if (type.isArrayType())
                p.print(" = {0}");

            p.print(";/* " + type + " */");
        }
    }
    


    /**
     * prints a method declaration
     */
    public void visitMethodDeclaration(JMethodDeclaration self,
                                       int modifiers,
                                       CType returnType,
                                       String ident,
                                       JFormalParameter[] parameters,
                                       CClassType[] exceptions,
                                       JBlock body) {
        
        //If this is the raw Main function then set is work to true
        //used for stack allocating arrays

        // try converting to macro
        if (MacroConversion.shouldConvert(self)) {
            MacroConversion.doConvert(self, isDeclOnly(), this);
            return;
        }
           
        p.newLine();
        // p.print(CModifier.toString(modifiers));
        printType(returnType);
        p.print(" ");
        //just print initPath() instead of initPath<Type>
        if (ident.startsWith("initPath"))
            p.print("initPath"); 
        else 
            p.print(ident);
        p.print("(");
        int count = 0;
        
        for (int i = 0; i < parameters.length; i++) {
            if (count != 0) {
                p.print(", ");
            }
            parameters[i].accept(this);
            count++;
        }
        p.print(")");
        
        //print the declaration then return
        if (isDeclOnly()) {
            p.print(";");
            return;
        }

        method = self;
        
        //set is init for dynamically allocating arrays...
        if (self.getName().startsWith("init"))
            isInit = true;

        p.print(" ");
        if (body != null) 
            body.accept(this);
        else 
            p.print(";");

        p.newLine();
        isInit = false;
        method = null;
    }

    // ----------------------------------------------------------------------
    // STATEMENT
    // ----------------------------------------------------------------------


    
    //for rate matching, we want to store the value of the item pushed  
    //into the output buffer and increment the sendbufferindex
    //args[0] is the item we want to push...
    private void rateMatchPush(JExpression[] args) 
    {
        p.print("(" + RawExecutionCode.sendBuffer);
        p.print("[++" + RawExecutionCode.sendBufferIndex + "] = ");
        args[0].accept(this);
        p.print(")");
    }
    

    
    /**
     * prints an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right) {

        //do not print class creation expression
        if (passParentheses(right) instanceof JQualifiedInstanceCreation ||
            passParentheses(right) instanceof JUnqualifiedInstanceCreation ||
            passParentheses(right) instanceof JQualifiedAnonymousCreation ||
            passParentheses(right) instanceof JUnqualifiedAnonymousCreation)
            return;

        //print the correct code for array assignment
        //this must be run after renaming!!!!!!
        if (left.getType() == null || right.getType() == null) {
            lastLeft=left;
            printLParen();
            left.accept(this);
            p.print(" = ");
            right.accept(this);
            printRParen();
            return;
        }

        if ((left.getType().isArrayType()) &&
            ((right.getType().isArrayType() || right instanceof SIRPopExpression) &&
             !(right instanceof JNewArrayExpression))) {
                    
            CArrayType type = (CArrayType)right.getType();
            String dims[] = Util.makeString(type.getDims());

            // dims should never be null now that we have static array
            // bounds
            assert dims != null;
            /*
            //if we cannot find the dim, just create a pointer copy
            if (dims == null) {
            lastLeft=left;
            printLParen();  // parenthesize if expr, not if stmt
            left.accept(this);
            p.print(" = ");
            right.accept(this);
            printRParen();
            return;
            }
            */

            p.print("{\n");
            p.print("int ");
            //print the index var decls
            for (int i = 0; i < dims.length -1; i++)
                p.print(RawExecutionCode.ARRAY_COPY + i + ", ");
            p.print(RawExecutionCode.ARRAY_COPY + (dims.length - 1));
            p.print(";\n");
            for (int i = 0; i < dims.length; i++) {
                p.print("for (" + RawExecutionCode.ARRAY_COPY + i + " = 0; " + RawExecutionCode.ARRAY_COPY + i +  
                        " < " + dims[i] + "; " + RawExecutionCode.ARRAY_COPY + i + "++)\n");
            }
            left.accept(this);
            for (int i = 0; i < dims.length; i++)
                p.print("[" + RawExecutionCode.ARRAY_COPY + i + "]");
            p.print(" = ");
            right.accept(this);
            for (int i = 0; i < dims.length; i++)
                p.print("[" + RawExecutionCode.ARRAY_COPY + i + "]");
            p.print(";\n}\n");
            return;
        }

        lastLeft=left;
        printLParen();  // parenthesize if expr, not if stmt
        left.accept(this);
        p.print(" = ");
        right.accept(this);
        printRParen();
    }

    

    /**
     * prints a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {
        /*
          if (ident != null && ident.equals(JAV_INIT)) {
          return; // we do not want generated methods in source code
          }
        */

        if (ident.equals(RawExecutionCode.gdnSendMethod) || 
                ident.equals(RawExecutionCode.staticSendMethod)) {
            handleSendMethod(self);
            return;
        }
            
        
        //generate the inline asm instruction to execute the 
        //receive if this is a receive instruction
        if (ident.equals(RawExecutionCode.gdnReceiveMethod) ||
                ident.equals(RawExecutionCode.staticReceiveMethod)) {
            //is this receive from the gdn
            boolean dynamicInput = ident.equals(RawExecutionCode.gdnReceiveMethod);
            
            if (args.length > 0) {
                visitArgs(args,0);
                p.print(" = ");
                p.print(Util.networkReceive
                        (dynamicInput, CommonUtils.getBaseType(self.getType())));
                
            }
            else {
                p.print(Util.networkReceive
                        (dynamicInput, CommonUtils.getBaseType(self.getType())));
            }
                        
            return;  
        }
        
        //we are receiving an array type, call the popArray method
        if (ident.equals(RawExecutionCode.arrayReceiveMethod)) {
            assert false : "Arrays over tapes not implemented!";
            return;
        }
        
        if (ident.equals(RawExecutionCode.rateMatchSendMethod)) {
            rateMatchPush(args);
            return;
        }
          
        p.print(ident);

        //we want single precision versions of the math functions raw
        //except for abs
        if (Utils.isMathMethod(prefix, ident) &&
                !ident.equals("abs")) 
            p.print("f");

        p.print("(");
        
        //if this method we are calling is the call to a structure 
        //receive method that takes a pointer, we have to add the 
        //address of operator
        if (ident.startsWith(RawExecutionCode.structReceiveMethodPrefix))
            p.print("&");

        int i = 0;
        /* Ignore prefix, since it's just going to be a Java class name.
           if (prefix != null) {
           prefix.accept(this);
           i++;
           }
        */
        visitArgs(args, i);
        p.print(")");
    }

    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
                                    JExpression num)
    {
        Utils.fail("TraceIRToC should see no peek expressions");
    }
    
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType)
    {
        Utils.fail("TraceIRToC should see no pop expressions");
    }
    
    public void visitPrintStatement(SIRPrintStatement self,
                                    JExpression exp)
    {
        //generate code to handle the print statement with a magic instruction
        //simulator callback
        RawSimulatorPrint.visitPrintStatement(self, exp, p, this);
    }
    
    private void pushScalar(boolean dynamicOutput,
            CType tapeType,
            JExpression val) 
    {

        //if this send is over the dynamic network, send the header
        //also send over opcode 13 to inform the dram that this is 
        //a data packet
        if (dynamicOutput) {
            p.print(Util.CGNOINTVAR + " = " + DYNMSGHEADER + ";");
            p.print(Util.networkSendPrefix(true, CStdType.Integer));
            p.print(RawChip.DRAM_GDN_DATA_OPCODE);
            p.print(Util.networkSendSuffix(true) + ";");
        }
        
        p.print(Util.networkSendPrefix(dynamicOutput, tapeType));
        //if the type of the argument to the push statement does not 
        //match the filter output type, print a cast.
        if (tapeType != val.getType())
            p.print("(" + tapeType + ")");
        val.accept(this);
        p.print(Util.networkSendSuffix(dynamicOutput));
        //useful if debugging...!
        /*print(";\n");
          p.print("raw_test_pass_reg(");
          val.accept(this);
          p.print(")");*/
    }

    
    public void pushClass(CType tapeType,
                JExpression val) 
    {
        assert false : "Structs over tapes unimplemented.";
        //turn the push statement into a call of
        //the structure's push method
        p.print("push" + tapeType + "(&");
        val.accept(this);
        p.print(")");
    }
    

    private void pushArray(CType tapeType,
                           JExpression val) 
    {
        assert false : "Arrays over tapes unimplemented.";
        CType baseType = ((CArrayType)tapeType).getBaseType();
        String dims[] = Util.makeString(((CArrayType)tapeType).getDims());
        
        for (int i = 0; i < dims.length; i++) {
            p.print("for (" + RawExecutionCode.ARRAY_INDEX + i + " = 0; " +
                    RawExecutionCode.ARRAY_INDEX + i + " < " + dims[i] + " ; " +
                    RawExecutionCode.ARRAY_INDEX + i + "++)\n");
        }

//        if(KjcOptions.altcodegen || KjcOptions.decoupled) {
//            p.print("{\n");
//            //      p.print(Util.CSTOVAR + " = ");
//            val.accept(this);
//            for (int i = 0; i < dims.length; i++) {
//                p.print("[" + RawExecutionCode.ARRAY_INDEX + i + "]");
//            }
//            p.print(";\n}\n");
//        } else {
            p.print("{");
            p.print("static_send((" + baseType + ") ");
            val.accept(this);
            for (int i = 0; i < dims.length; i++) {
                p.print("[" + RawExecutionCode.ARRAY_INDEX + i + "]");
            }
            p.print(");\n}\n");
//        }
    }
    
    public void visitPushExpression(SIRPushExpression self,
                                    CType tapeType,
                                    JExpression val)
    {
        assert false : "TraceIRToC should not see any SIRPushExpression.";
    }
    
    private void handleSendMethod(JMethodCallExpression methCall)
    {
        assert methCall.getArgs().length == 1 : "Malformed send method encountered.";
        
        if (methCall.getType().isArrayType())
            pushArray(methCall.getType(), methCall.getArgs()[0]);
        else if (methCall.getType().isClassType())
            pushClass(methCall.getType(), methCall.getArgs()[0]);
        else  {
            //otherwise call push scalar 
            //we know based on the method name if it is over the gdn or static
            pushScalar((methCall.getIdent().equals(RawExecutionCode.gdnSendMethod) ? 
                        true : false), 
                    methCall.getType(), 
                    methCall.getArgs()[0]);
        }
    }
    
    public void visitRegReceiverStatement(SIRRegReceiverStatement self,
                                          JExpression portal,
                                          SIRStream receiver, 
                                          JMethodDeclaration[] methods)
    {
        p.print("register_receiver(");
        portal.accept(this);
        p.print(", data->context, ");
        p.print(self.getItable().getVarDecl().getIdent());
        p.print(", LATENCY_BEST_EFFORT);");
        // (But shouldn't there be a latency field in here?)
    }
    
    public void visitRegSenderStatement(SIRRegSenderStatement self,
                                        String fn,
                                        SIRLatency latency)
    {
        p.print("register_sender(this->context, ");
        p.print(fn);
        p.print(", ");
        latency.accept(this);
        p.print(");");
    }

    /**
     * prints InlineAssembly code
     */
    public void visitInlineAssembly(InlineAssembly self,String[] asm,String[] input,String[] clobber) {
        p.print("asm volatile(\"");
        if(asm.length>0)
            p.print(asm[0]);
        for(int i=1;i<asm.length;i++)
            p.print("\\n\\t"+asm[i]);
        p.print("\"::");
        if(input.length>0)
            p.print(input[0]);
        for(int i=1;i<input.length;i++)
            p.print(","+input[i]);
        if(clobber.length>0) {
            p.print(":\""+clobber[0]);
            for(int i=1;i<clobber.length;i++)
                p.print("\",\""+clobber[i]);
            p.print("\"");
        }
        p.print(");");
    }

}
