package at.dms.kjc.raw;

import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.FlatNode;
//import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
//import java.util.List;
//import java.util.ListIterator;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.HashMap;
import java.io.*;
//import at.dms.compiler.*;
//import at.dms.kjc.sir.lowering.*;
//import java.util.Hashtable;
//import at.dms.util.SIRPrinter;
import at.dms.kjc.sir.lowering.FinalUnitOptimize;
/**
 * This class dumps the tile code for each filter into a file based 
 * on the tile number assigned 
 */
public class FlatIRToC extends ToC implements StreamVisitor
{
    private boolean DEBUG = false;
    //the filter we are currently visiting
    private SIRFilter filter;

    // ALWAYS!!!!
    //true if we are using the second buffer management scheme 
    //circular buffers with anding
    public boolean debug = false;//true;
    /** > 0 if in a for loop header during visit **/
    private int forLoopHeader = 0;

    //fields for all of the vars names we introduce in the c code
    private final String FLOAT_HEADER_WORD = "__FLOAT_HEADER_WORD__";
    private final String INT_HEADER_WORD = "__INT_HEADER_WORD__";

    //variable names for iteration counter for --standalone option...
    public static String MAINMETHOD_ARGC    = "argc";
    public static String MAINMETHOD_ARGV    = "argv";
    public static String MAINMETHOD_COUNTER = "__iterationCounter";
    public static String ARGHELPER_COUNTER  = "__setIterationCounter";

    private static int filterID = 0;
    
    public static void generateCode(FlatNode node) 
    {
        SIRFilter str = (SIRFilter)node.contents;
        FlatIRToC toC = new FlatIRToC(str);
        //FieldInitMover.moveStreamInitialAssignments((SIRFilter)node.contents);
        //FieldProp.doPropagate((SIRFilter)node.contents);

        //Optimizations
        
        
            System.out.println
                ("Optimizing "+
                 ((SIRFilter)node.contents).getName()+"...");

            (new FinalUnitOptimize(){
                protected boolean optimizeThisMethod(SIRCodeUnit unit, JMethodDeclaration method) {
                    return !(method.getName().startsWith("work")||method.getName().startsWith("initWork"));
                }
            }).optimize((SIRFilter)node.contents);

        IterFactory.createFactory().createIter((SIRFilter)node.contents).accept(toC);
    }
    
    public FlatIRToC() 
    {
        super();
    }
    

    public FlatIRToC(CodegenPrintWriter p) {
        super(p);
    }
    
    public FlatIRToC(SIRFilter f) {
        super();
        this.filter = f;
        //      circular = false;
    }
    
    

    /*  
        public void visitStructure(SIRStructure self,
        SIRStream parent,
        JFieldDeclaration[] fields)
        {
        p.print("struct " + self.getIdent() + " {\n");
        for (int i = 0; i < fields.length; i++)
        fields[i].accept(this);
        p.print("};\n");
        }
    */
    
    public void visitFilter(SIRFilter self,
                            SIRFilterIter iter) {

        //       System.out.println(self.getName());
        
        //Entry point of the visitor

        //do not print the raw header if compiling
        //for uniprocessor
        if (!KjcOptions.standalone) 
            p.print("#include <raw.h>\n");
        p.print("#include <stdlib.h>\n");
        p.print("#include <math.h>\n\n");

        p.print(RawSimulatorPrint.getHeader());
        
        //if we are number gathering and this is the sink, generate the dummy
        //vars for the assignment of the print expression.
        if (KjcOptions.numbers > 0) {
            p.print("volatile int dummyInt;\n");
            p.print("volatile float dummyFloat;\n");
        }
        
        if (/*KjcOptions.altcodegen && */ !KjcOptions.decoupled){
            p.print("register float " + RawUtil.CSTOFPVAR + " asm(\"$csto\");\n");
            p.print("register float " + RawUtil.CSTIFPVAR + " asm(\"$csti\");\n");
            p.print("register int " + RawUtil.CSTOINTVAR + " asm(\"$csto\");\n");
            p.print("register int " + RawUtil.CSTIINTVAR + " asm(\"$csti\");\n");
        }
        
        if (KjcOptions.decoupled) {
            p.print("volatile float " + RawUtil.CSTOFPVAR + ";\n");
            p.print("volatile float " + RawUtil.CSTIFPVAR + ";\n");
            p.print("volatile int " + RawUtil.CSTOINTVAR + ";\n");
            p.print("volatile int " + RawUtil.CSTIINTVAR + ";\n");
        }
        
        if (RawBackend.FILTER_DEBUG_MODE) {
            p.print("void static_send_print(");
            if (self.getOutputType() == CStdType.Void)
                p.print("int f) {\n");            
            else 
                p.print(self.getOutputType() + " f) {\n");
            if (self.getOutputType().isFloatingPoint()) 
                p.print("print_float(f);\n");
            else 
                p.print("print_int(f);\n");
            p.print("static_send(f);\n");
            p.print("}\n\n");
        }
        
        //if there are structures in the code, include
        //the structure definition header files
        SIRStructure[] structs = RawBackend.structures;
        if (RawWorkEstimator.SIMULATING_WORK)
            structs = RawWorkEstimator.structures;
        
        if (structs.length > 0) 
            p.print("#include \"structs.h\"\n");

        //print the extern for the function to init the 
        //switch, do not do this if we are compiling for
        //a uniprocessor
        if (!KjcOptions.standalone) {
            p.print("void raw_init();\n");
            p.print("void " + SwitchCode.SW_SS_TRIPS + "();\n");
        }

        //Visit fields declared in the filter class
        JFieldDeclaration[] fields = self.getFields();
        for (int i = 0; i < fields.length; i++)
            fields[i].accept(this);
        
        //add function for --standalone that will get the iteration
        //count from the command-line
        if (KjcOptions.standalone) 
            addIterCountFunction();

        //visit methods of filter, print the declaration first
        setDeclOnly(true);
        JMethodDeclaration[] methods = self.getMethods();
        for (int i =0; i < methods.length; i++)
            methods[i].accept(this);
        //now print the functions with body
        setDeclOnly(false);
        for (int i =0; i < methods.length; i++) {
            methods[i].accept(this);    
        }
        
        if (KjcOptions.decoupled) {
            p.print("void raw_init2() {}");
        }
            
        
        //if we are generating raw code print the begin
        //method for the simulator
        if (!KjcOptions.standalone) {
            p.print("void begin(void) {\n");
        }
        else {
            //otherwise print a normal main()
            p.print("int main(int argc, char** argv) {\n");
        }
      
        if (KjcOptions.standalone) {
            p.print("  " + ARGHELPER_COUNTER + "(argc, argv);\n");
        }
        

        //if we are using the magic network, 
        //use a magic instruction to initialize the magic fifos
        if (KjcOptions.magic_net)
            p.print("  __asm__ volatile (\"magc $0, $0, 1\");\n");
        
        //initialize the dummy network receive value
        if (KjcOptions.decoupled) {
            if (self.getInputType().isFloatingPoint()) 
                p.print("  " + RawUtil.CSTIFPVAR + " = 1.0;\n");
            else 
                p.print("  " + RawUtil.CSTIINTVAR + " = 1;\n");
        }

        //call the raw_init() function for the static network
        //only if we are not using a uniprocessor or the
        //magic network
        if (!(KjcOptions.standalone || KjcOptions.magic_net || KjcOptions.decoupled ||
              IMEMEstimation.TESTING_IMEM)) {
            p.print("  raw_init();\n");
            //the call to raw_init2 is now within __RAW_MAIN__()
        }
        //execute the raw main function
        p.print(RawExecutionCode.rawMain + "();\n");
        //return 0 if we are generating normal c code
        if (KjcOptions.standalone) 
            p.print("  return 0;\n");
        p.print("}\n");
       
        createFile();
    }

    //generate a function that will get the iteration count from the 
    //command-line, only generation this if --standalone is enabled...
    private void addIterCountFunction() 
    {
        p.print("\n/* helper routines to parse command line arguments */\n");
        p.print("#include <unistd.h>\n\n");

        p.print("/* retrieve iteration count for top level driver */\n");
        p.print("static void " + ARGHELPER_COUNTER + "(int argc, char** argv) {\n");
        p.print("    int flag;\n");
        p.print("    while ((flag = getopt(argc, argv, \"i:\")) != -1)\n");
        p.print("       if (flag == \'i\') { " + MAINMETHOD_COUNTER + " =  atoi(optarg); return;}\n");
        p.print("    " + MAINMETHOD_COUNTER + " = -1; /* default iteration count (run indefinitely) */\n");
        p.print("}\n\n\n");
    }
    

    public void visitPhasedFilter(SIRPhasedFilter self,
                                  SIRPhasedFilterIter iter) {
        // This is a stub; it'll get filled in once we figure out how phased
        // filters should actually work.
    }

    private void createFile() {
        System.out.println("Code for " + filter.getName() +
                           " written to tile" + Layout.getTileNumber(filter) +
                           ".c");
        try {
            FileWriter fw = new FileWriter("tile" + Layout.getTileNumber(filter) + ".c");
            fw.write(p.getString().toString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write tile code file for filter " +
                               filter.getName());
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
        
        //be careful, if you return prematurely, decrement me
        forLoopHeader++;

        p.print("for (");
        if (init != null) {
            init.accept(this);
            //the ; will print in a statement visitor
        }

        p.print(" ");
        if (cond != null) {
            cond.accept(this);
        }
        //cond is an expression so print the ;
        p.print("; ");
        if (incr != null) {
            FlatIRToC l2c = new FlatIRToC(filter);
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
        forLoopHeader--;
        p.print(") ");

        p.print("{");
        p.indent();
        body.accept(this);
        p.outdent();
        p.newLine();
        p.print("}");
    }

   
    /**
     * prints a empty statement
     */
    public void visitEmptyStatement(JEmptyStatement self) {
        //if we are inside a for loop header, we need to print 
        //the ; of an empty statement
        if (forLoopHeader > 0) {
            p.newLine();
            p.print(";");
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
        //System.out.println(ident);
        
        //in the raw path we do not want to print the 
        //prework or work function definition
        if (filter != null && 
            (filter.getWork().equals(self) ||
             (filter instanceof SIRTwoStageFilter &&
              ((SIRTwoStageFilter)filter).getInitWork().equals(self))))
            return;

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

        if (IMEMEstimation.TESTING_IMEM && 
            self.getName().startsWith("init")) {
            //just print a null method
            p.print("{}\n");
            return;
        }
        
        method = self;
        
        //set is init for dynamically allocating arrays...
        if (filter != null &&
            self.getName().startsWith("init"))
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

    /* //appears not to be used
       private void dummyWork(int push) {
       p.print("{\n");
       p.print("  int i;\n");
       p.print("  for(i = 0; i < " + push + "; i++)\n");
       p.print("    static_send(i);\n");
       p.print("}\n");
       }
    */
    /* //appears not to be used
       private int nextPow2(int i) {
       String str = Integer.toBinaryString(i);
       if  (str.indexOf('1') == -1)
       return 0;
       int bit = str.length() - str.indexOf('1');
       return (int)Math.pow(2, bit);
       }
    */    
    // ----------------------------------------------------------------------
    // STATEMENT
    // ----------------------------------------------------------------------


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
            } else if (RawWorkEstimator.SIMULATING_WORK && ident.indexOf(RawExecutionCode.recvBuffer)!=-1) {
                // this is to prevent partial evaluation of inputs to
                // filter by C compiler if we are trying to simulate work
                // in a node
                if (type.isOrdinal())
                    p.print(" = " + ((int)Math.random()));
                else if (type.isFloatingPoint()) {
                    p.print(" = " + ((float)Math.random()) + "f");
                }
            } 
            else if (type.isOrdinal())
                p.print(" = 0");
            else if (type.isFloatingPoint())
                p.print(" = 0.0f");
            else if (type.isArrayType()) 
                p.print(" = {0}");

            p.print(";/* " + type + " */");
        }
    }

    /**
     * This should never be called 
     * Generates code to receive an array type into the buffer
     **/
    /*
      public void popArray(JExpression arg) 
      {
      String dims[] = Util.makeString(((CArrayType)filter.getInputType()).getDims());
        
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
      p.print(Util.staticNetworkReceiveSuffix(((CArrayType)filter.getInputType()).getBaseType()));
      p.print("}");
      }
    */
    
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

        // copy arrays element-wise
        boolean arrayType = ((left.getType()!=null && left.getType().isArrayType()) ||
                             (right.getType()!=null && right.getType().isArrayType()));
        if (arrayType && !(right instanceof JNewArrayExpression)) {
                    
            CArrayType type = (CArrayType)right.getType();
            String dims[] = this.makeArrayStrings(type.getDims());

            // dims should never be null now that we have static array
            // bounds
            assert dims != null;
            /*
            //if we cannot find the dim, just create a pointer copy
            if (dims == null) {
            lastLeft=left;
            printLParen();
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
        printLParen();
        left.accept(this);
        p.print(" = ");
        right.accept(this);
        printRParen();
        // would it catch other bugs to put lastLeft=null here?
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

        //generate the inline asm instruction to execute the 
        //receive if this is a receive instruction
        if (ident.equals(RawExecutionCode.receiveMethod)) {
            p.print(RawUtil.staticNetworkReceivePrefix());
            visitArgs(args,0);
            p.print(RawUtil.staticNetworkReceiveSuffix
                    (CommonUtils.getBaseType(filter.getInputType())));
            return;  
        }

        /*      
        //we are receiving an array type, call the popArray method
        if (ident.equals(RawExecutionCode.arrayReceiveMethod)) {
        popArray(args[0]);
        return;
        }
        */
        /*
          if (ident.equals(RawExecutionCode.rateMatchSendMethod)) {
          rateMatchPush(args);
          return;
          }
        */
        p.print(ident);
        
        //we want single precision versions of the math functions
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

    public void visitDynamicToken(SIRDynamicToken self) {
        Utils.fail("Dynamic rates not yet supported in cluster backend.");
    }

    public void visitRangeExpression(SIRRangeExpression self) {
        Utils.fail("Dynamic rates not yet supported in cluster backend.");
    }

    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
                                    JExpression num)
    {
        Utils.fail("FlatIRToC should see no peek expressions");
    }
    
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType)
    {
        Utils.fail("FlatIRToC should see no pop expressions");
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
        
        //if we have the number gathering stuff on, convert each print 
        //to a magic instruction, there are only print statements in the sink
        //all other prints have been removed...
        if (KjcOptions.numbers > 0) {
            //assign the expression to a dummy var do it does not get
            //optimized out...
            p.print("dummy");
            if (type.isFloatingPoint())
                p.print("Float");
            else 
                p.print("Int");
            p.print(" = ");
            exp.accept(this);
            p.print(";\n");
            p.print("__asm__ volatile (\"magc $0, $0, 2\");\n");
            return;
        }

        //generate code to handle the print statement with a magic instruction
        //simulator callback
        RawSimulatorPrint.visitPrintStatement(self, exp, p, this);
    }
    
    private void pushScalar(SIRPushExpression self,
                            CType tapeType,
                            JExpression val) 
    {
        //      if (tapeType != val.getType()) {
        //    Utils.fail("type of push argument does not match filter output type");
        //      }
        p.print(RawUtil.staticNetworkSendPrefix(tapeType));
        //if the type of the argument to the push statement does not 
        //match the filter output type, print a cast.
        if (tapeType != val.getType())
            p.print("(" + tapeType + ")");
        val.accept(this);
        p.print(RawUtil.staticNetworkSendSuffix());
        //useful if debugging...!
        /*print(";\n");
          p.print("raw_test_pass_reg(");
          val.accept(this);
          p.print(")");*/
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
        String dims[] = this.makeArrayStrings(((CArrayType)tapeType).getDims());
        String ARRAY_INDEX = "ARRAY_PUSH_INDEX";

        p.print("{\n");
        p.print("  int ");
        for (int i = 0; i < dims.length; i++) {
            p.print(ARRAY_INDEX + i);
            if (i < dims.length - 1)
                p.print(", ");
        }
        p.print(";\n");
        for (int i = 0; i < dims.length; i++) {
            p.print("  for (" + ARRAY_INDEX + i + " = 0; " +
                    ARRAY_INDEX + i + " < " + dims[i] + " ; " +
                    ARRAY_INDEX + i + "++)\n");
        }

//        if(KjcOptions.altcodegen || KjcOptions.decoupled) {
            p.print("{\n");
            p.print(RawUtil.staticNetworkSendPrefix(CommonUtils.getBaseType(tapeType)));
            val.accept(this);
            for (int i = 0; i < dims.length; i++) {
                p.print("[" + ARRAY_INDEX + i + "]");
            }
            p.print(RawUtil.staticNetworkSendSuffix());
            p.print(";\n}\n");
//        } else {
//            p.print("{");
//            p.print("static_send((" + baseType + ") ");
//            val.accept(this);
//            for (int i = 0; i < dims.length; i++) {
//                p.print("[" + ARRAY_INDEX + i + "]");
//            }
//            p.print(");\n}\n");
//        }
        p.print("}\n");
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


    // ----------------------------------------------------------------------
    // UNUSED STREAM VISITORS
    // ----------------------------------------------------------------------

    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
                                 SIRPipelineIter iter) 
    {
    }
    

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
                                  SIRSplitJoinIter iter)
    {
    }
    

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
                                     SIRFeedbackLoopIter iter)
    {
    }
    

    /**
     * POST-VISITS 
     */
            
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
                                  SIRPipelineIter iter) {
    }
    

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
                                   SIRSplitJoinIter iter) {
    }
    

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
                                      SIRFeedbackLoopIter iter) {
    }
}
