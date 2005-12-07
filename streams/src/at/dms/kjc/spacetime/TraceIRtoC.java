package at.dms.kjc.spacetime;

import at.dms.kjc.common.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import at.dms.util.SIRPrinter;
//import at.dms.compiler.*;

/**
 * This class returns the c code (a string) for a given raw tile
 */
public class TraceIRtoC extends ToC
{
    //the raw tile we are generating code for
    private RawTile tile;
    
    public TraceIRtoC(RawTile tile) 
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
        optimizations();
        
        //write header 
        generateHeader();
        
        //generate the fields
        for (int i = 0; i < tile.getComputeCode().getFields().length; i++)
            tile.getComputeCode().getFields()[i].accept(this);
        
        p.print("\n");
        
        //print the pointers to the off chip buffers
        if (!KjcOptions.magicdram)
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
        
        //print the method that sends/recvs block addresses
        if (!KjcOptions.magicdram) {
            p.print(CommunicateAddrs.getFunction(tile));
            //print(CommunicateAddrs.getFreeFunction(tile));
        }
        
        //generate the entry function for the simulator
        p.print("void begin(void) {\n");
        //if we are using the magic network, 
        //use a magic instruction to initialize the magic fifos
        if (KjcOptions.magic_net)
            p.print("  __asm__ volatile (\"magc $0, $0, 1\");\n");

        //call the raw_init() function for the static network
        if (!KjcOptions.decoupled && !KjcOptions.magic_net) {
            p.print("  raw_init();\n");
            //print("  raw_init2();\n");
        }

        if (!KjcOptions.magicdram) 
            p.print("  " + CommunicateAddrs.functName + "();\n");
        
        //print(tile.getComputeCode().getMainFunction().getName() + "();\n");
        method = mainMethod;
        mainMethod.getBody().accept(this); //Inline Main method
        method = null;
        p.print("};\n");
    }
        
    private void optimizations() 
    {
        ArrayDestroyer arrayDest=new ArrayDestroyer();
        for (int i = 0; i < tile.getComputeCode().getMethods().length; i++) {
            if (!KjcOptions.nofieldprop) {
                 Unroller unroller;
                 do {
                     do {
                         unroller = new Unroller(new Hashtable());
                         tile.getComputeCode().getMethods()[i].accept(unroller);
                     } while(unroller.hasUnrolled());
                     tile.getComputeCode().getMethods()[i].accept(new Propagator(new Hashtable()));
                     unroller = new Unroller(new Hashtable());
                     tile.getComputeCode().getMethods()[i].accept(unroller);
                 } while(unroller.hasUnrolled());
                 tile.getComputeCode().getMethods()[i].accept(new BlockFlattener());
                 tile.getComputeCode().getMethods()[i].accept(new Propagator(new Hashtable()));
             } else
                 tile.getComputeCode().getMethods()[i].accept(new BlockFlattener());
             
            //tile.getComputeCode().getMethods()[i].accept(arrayDest);
             tile.getComputeCode().getMethods()[i].accept(new VarDeclRaiser());
        }
    }
    
    private void generateHeader() 
    {
        p.print("#include <raw.h>\n");
        p.print("#include <stdlib.h>\n");
        p.print("#include <math.h>\n\n");

        //if we are number gathering and this is the sink, generate the dummy
        //vars for the assignment of the print expression.
        /*
          if (KjcOptions.numbers > 0 && NumberGathering.successful &&
          self == NumberGathering.sink.contents) {
          p.print("int dummyInt;\n");
          p.print("float dummyFloat;\n");
          }
        */
        
        if (KjcOptions.altcodegen && !KjcOptions.decoupled){
            p.print("register float " + Util.CSTOFPVAR + " asm(\"$csto\");\n");
            p.print("register float " + Util.CSTIFPVAR + " asm(\"$csti\");\n");
            p.print("register int " + Util.CSTOINTVAR + " asm(\"$csto\");\n");
            p.print("register int " + Util.CSTIINTVAR + " asm(\"$csti\");\n");
            p.print("register int " + Util.CGNOINTVAR + " asm(\"$cgno\");\n");
        }
        
        if (KjcOptions.decoupled) {
            p.print("float " + Util.CSTOFPVAR + ";\n");
            p.print("float " + Util.CSTIFPVAR + ";\n");
            p.print("int " + Util.CSTOINTVAR + ";\n");
            p.print("int " + Util.CSTIINTVAR + ";\n");
        }
        
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


     /**
     * prints a for statement
     */
    public void visitForStatement(JForStatement self,
                                  JStatement init,
                                  JExpression cond,
                                  JStatement incr,
                                  JStatement body) {
        p.print("for (");

        boolean oldStatementContext = statementContext;
        statementContext = false; // initially exprs, with ';' forced

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

        statementContext = true;
        p.print("{");
        p.indent();
        body.accept(this);
        p.outdent();
        p.newLine();
        p.print("}");
        statementContext = oldStatementContext;
    }

    protected void stackAllocateArray(String ident){
        //find the dimensions of the array!!
        String dims[] = 
            ArrayDim.findDim(new TraceIRtoC(), tile.getComputeCode().getFields(), method, ident);
        
        for (int i = 0; i < dims.length; i++)
            p.print("[" + dims[i] + "]");
        return;
    }
    
     /**
     * prints a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {
        //we want to stack allocate all arrays
        //we convert an assignment statement into the stack allocation statement'
        //so, just remove the var definition, if the new array expression
        //is not included in this definition, just remove the definition,
        //when we visit the new array expression we will print the definition...
        if (type.isArrayType() && !isInit) {
            String[] dims = ArrayDim.findDim(new TraceIRtoC(), tile.getComputeCode().getFields(), method, ident);
            //but only do this if the array has corresponding 
            //new expression, otherwise don't print anything.
            if (expr instanceof JNewArrayExpression) {
                //print the type
                typePrint(((CArrayType)type).getBaseType());
                p.print(" ");
                //print the field identifier
                p.print(ident);
                //print the dims
                stackAllocateArray(ident);
                p.print(";");
                return;
            }
            else if (dims != null)
                return;
            else if (expr instanceof JArrayInitializer) {
                declareInitializedArray(type, ident, expr);
                return;
            }
        }
        
        typePrint(type);

        p.print(" ");
        p.print(ident);
        if (expr != null) {
            p.print(" = ");
            expr.accept(this);
        }
        else if (type.isOrdinal())
            p.print(" = 0");
        else if (type.isFloatingPoint())
            p.print(" = 0.0f");

        p.print(";\n");

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
        typePrint(returnType);
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


    /**
     * Generates code to receive an array type into the buffer
     **/
    public void popArray(JExpression arg, CArrayType type) 
    {
        String dims[] = Util.makeString(type.getDims());
        
        //print the array indices
        for (int i = 0; i < dims.length; i++) {
            p.print("for (" + RawExecutionCode.ARRAY_INDEX + i + " = 0; " +
                  RawExecutionCode.ARRAY_INDEX + i + " < " + dims[i] + " ; " +
                  RawExecutionCode.ARRAY_INDEX + i + "++)\n");
        }
        
        p.print("{");
        //print out the receive assembly
        p.print(Util.staticNetworkReceivePrefix());
        //print out the buffer variable and the index
        arg.accept(this);
        //now append the remaining dimensions
        for (int i = 0; i < dims.length; i++) {
                p.print("[" + RawExecutionCode.ARRAY_INDEX + i + "]");
            }
        //finish up the receive assembly
        p.print(Util.staticNetworkReceiveSuffix(type.getBaseType()));
        p.print("}");
    }
    
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
            boolean oldStatementContext = statementContext;
            lastLeft=left;
            printLParen();
            statementContext = false;
            left.accept(this);
            p.print(" = ");
            right.accept(this);
            statementContext = oldStatementContext;
            printRParen();
            return;
        }

        if ((left.getType().isArrayType()) &&
            ((right.getType().isArrayType() || right instanceof SIRPopExpression) &&
             !(right instanceof JNewArrayExpression))) {
                    
            String ident = "";
                    
            if (left instanceof JFieldAccessExpression) 
                ident = ((JFieldAccessExpression)left).getIdent();
            else if (left instanceof JLocalVariableExpression) 
                ident = ((JLocalVariableExpression)left).getVariable().getIdent();
            else 
                Utils.fail("Assigning an array to an unsupported expression of type " + left.getClass() + ": " + left);
            
            String[] dims = ArrayDim.findDim(new TraceIRtoC(), tile.getComputeCode().getFields(), method, ident);
            //if we cannot find the dim, just create a pointer copy
            if (dims == null) {
                boolean oldStatementContext = statementContext;
                lastLeft=left;
                printLParen();  // parenthesize if expr, not if stmt
                statementContext = false;
                left.accept(this);
                p.print(" = ");
                right.accept(this);
                statementContext = oldStatementContext;
                printRParen();
                return;
            }
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

        //stack allocate all arrays when not in init function
        //done at the variable definition
        if (right instanceof JNewArrayExpression &&
            (left instanceof JLocalVariableExpression) && !isInit) {
            //      (((CArrayType)((JNewArrayExpression)right).getType()).getArrayBound() < 2)) {

            //get the basetype and print it 
            CType baseType = ((CArrayType)((JNewArrayExpression)right).getType()).getBaseType();
            typePrint(baseType);
            p.print(" ");
            //print the identifier
            left.accept(this);
            //print the dims of the array
            String ident;
            ident = ((JLocalVariableExpression)left).getVariable().getIdent();
            stackAllocateArray(ident);
            return;
        }
           

        
        boolean oldStatementContext = statementContext;
        lastLeft=left;
        printLParen();  // parenthesize if expr, not if stmt
        statementContext = false;
        left.accept(this);
        p.print(" = ");
        right.accept(this);
        statementContext = oldStatementContext;
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


        //supress the call to memset if the array is of size 0
        //if (ident.equals("memset")) {
        //    String[] dims = ArrayDim.findDim(filter, ((JLocalVariableExpression)args[0]).getIdent());
        //    if (dims[0].equals("0"))
        //      return;
        //}

        //generate the inline asm instruction to execute the 
        //receive if this is a receive instruction
        if (ident.equals(RawExecutionCode.receiveMethod)) {
            p.print(Util.staticNetworkReceivePrefix());
            visitArgs(args,0);
            p.print(Util.staticNetworkReceiveSuffix(self.getType()));
            return;  
        }
        
        //we are receiving an array type, call the popArray method
        if (ident.equals(RawExecutionCode.arrayReceiveMethod)) {
            popArray(args[0], (CArrayType)self.getType());
            return;
        }
        
        if (ident.equals(RawExecutionCode.rateMatchSendMethod)) {
            rateMatchPush(args);
            return;
        }
          
        p.print(ident);

        //we want single precision versions of the math functions raw
        if (Utils.isMathMethod(prefix, ident)) 
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
        CType type = null;

        
        try {
            type = exp.getType();
        }
        catch (Exception e) {
            System.err.println("Cannot get type for print statement");
            type = CStdType.Integer;
        }
        
        if (type.equals(CStdType.Boolean))
            {
                Utils.fail("Cannot print a boolean");
            }
        else if (type.equals(CStdType.Byte) ||
                 type.equals(CStdType.Integer) ||
                 type.equals(CStdType.Short))
            {
                //print("print_int(");
                p.print("raw_test_pass_reg(");

                //print("gdn_send(" + INT_HEADER_WORD + ");\n");
                //print("gdn_send(");
                exp.accept(this);
                p.print(");");
            }
        else if (type.equals(CStdType.Char))
            {
                p.print("raw_test_pass_reg(");
                //print("print_int(");

                //print("gdn_send(" + INT_HEADER_WORD + ");\n");
                //print("gdn_send(");
                exp.accept(this);
                p.print(");");
            }
        else if (type.equals(CStdType.Float))
            {
                p.print("raw_test_pass_reg(");
                //print("print_float(");

                //print("gdn_send(" + FLOAT_HEADER_WORD + ");\n");
                //print("gdn_send(");
                exp.accept(this);
                p.print(");");
            }
        else if (type.equals(CStdType.Long))
            {
                p.print("raw_test_pass_reg(");
                //print("print_int(");
                //              p.print("gdn_send(" + INT_HEADER_WORD + ");\n");
                //print("gdn_send(");
                exp.accept(this);
                p.print(");");
            }
        else if (type.equals(CStdType.String)) 
            {
                p.print("print_string(");
                //              p.print("gdn_send(" + INT_HEADER_WORD + ");\n");
                //print("gdn_send(");
                exp.accept(this);
                p.print(");");
            }
        else
            {
                System.out.println("Unprintable type");
                p.print("print_int(");
                exp.accept(this);
                p.print(");");
                //Utils.fail("Unprintable Type");
            }
    }
    
    private void pushScalar(SIRPushExpression self,
                            CType tapeType,
                            JExpression val) 
    {
        p.print(Util.staticNetworkSendPrefix(tapeType));
        val.accept(this);
        p.print(Util.staticNetworkSendSuffix());
    }

    
    public void pushClass(SIRPushExpression self, 
                          CType tapeType,
                          JExpression val) 
    {
        //turn the push statement into a call of
        //the structure's push method
        p.print("push" + tapeType + "(&");
        val.accept(this);
        p.print(")");
    }
    

    private void pushArray(SIRPushExpression self, 
                           CType tapeType,
                           JExpression val) 
    {
        CType baseType = ((CArrayType)tapeType).getBaseType();
        String dims[] = Util.makeString(((CArrayType)tapeType).getDims());
        
        for (int i = 0; i < dims.length; i++) {
            p.print("for (" + RawExecutionCode.ARRAY_INDEX + i + " = 0; " +
                  RawExecutionCode.ARRAY_INDEX + i + " < " + dims[i] + " ; " +
                  RawExecutionCode.ARRAY_INDEX + i + "++)\n");
        }

        if(KjcOptions.altcodegen || KjcOptions.decoupled) {
            p.print("{\n");
            //      p.print(Util.CSTOVAR + " = ");
            val.accept(this);
            for (int i = 0; i < dims.length; i++) {
                p.print("[" + RawExecutionCode.ARRAY_INDEX + i + "]");
            }
            p.print(";\n}\n");
        } else {
            p.print("{");
            p.print("static_send((" + baseType + ") ");
            val.accept(this);
            for (int i = 0; i < dims.length; i++) {
                p.print("[" + RawExecutionCode.ARRAY_INDEX + i + "]");
            }
            p.print(");\n}\n");
        }
    }
    
    public void visitPushExpression(SIRPushExpression self,
                                    CType tapeType,
                                    JExpression val)
    {
        if (tapeType.isArrayType())
            pushArray(self, tapeType, val);
        else if (tapeType.isClassType())
            pushClass(self, tapeType, val);
        else 
            pushScalar(self, tapeType, val);
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
