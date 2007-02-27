package at.dms.kjc.spacedynamic;

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
//import java.util.HashSet;
import java.io.*;
//import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
//import java.util.Hashtable;
//import at.dms.util.SIRPrinter;

/**
 * This class dumps the tile code for each filter into a file based 
 * on the tile number assigned 
 */
public class FlatIRToC extends ToC implements StreamVisitor
{
    private boolean DEBUG = false;
    //the filter we are currently visiting
    private SIRFilter filter;

    public boolean debug = false;//true;
    /** > 0 if in a for loop header during visit **/
    private int forLoopHeader = 0;

    //fields for all of the vars names we introduce in the c code
    private final String FLOAT_HEADER_WORD = "__FLOAT_HEADER_WORD__";
    private final String INT_HEADER_WORD = "__INT_HEADER_WORD__";
    public static final String DYNMSGHEADER = "__DYNMSGHEADER__";

    private static int filterID = 0;
    
    //variable names for iteration counter for --standalone option...
    public static String MAINMETHOD_ARGC    = "argc";
    public static String MAINMETHOD_ARGV    = "argv";
    public static String MAINMETHOD_COUNTER = "__iterationCounter";
    public static String ARGHELPER_COUNTER  = "__setIterationCounter";

    //the flat node for the filter we are visiting
    private FlatNode flatNode;

    private Layout layout;
    /** true if the filter is the sink of a SSG, so it has dynamic output 
        and must sent output over the dynamic network **/
    private boolean dynamicOutput = false;
    private boolean dynamicInput = false;
    private SpdStaticStreamGraph ssg;

    private static String ARRAY_INIT_PREFIX = "init_array";

    //for the first filter/tile we encounter we are going to 
    //create a magic instruction that tells the number-gathering
    //stuff that everything is done snake booting, so if this is true
    //don't generate the magic instruction
    private static boolean gen_magc_done_boot = false;

    public static void generateCode(SpdStaticStreamGraph SSG, FlatNode node)
    {
        assert Layout.assignToATile(node);
        SIRFilter str = (SIRFilter)node.contents;
        
        FlatIRToC toC = new FlatIRToC(str);
        toC.flatNode = node;
        toC.ssg = SSG;
        toC.layout = ((SpdStreamGraph)SSG.getStreamGraph()).getLayout();
        toC.dynamicOutput = toC.ssg.isOutput(node) || toC.ssg.simulator instanceof NoSimulator;
        toC.dynamicInput = toC.ssg.isInput(node) || toC.ssg.simulator instanceof NoSimulator;
    
        //FieldInitMover.moveStreamInitialAssignments((SIRFilter)node.contents);
        //FieldProp.doPropagate((SIRFilter)node.contents);

        //Optimizations
        
        
        //System.out.println
        //    ("Optimizing "+
        //     ((SIRFilter)node.contents).getName()+"...");

        (new FinalUnitOptimize(){
            protected boolean optimizeThisMethod(SIRCodeUnit unit, JMethodDeclaration method) {
                //don't do anything for the work function if it is inlined or for the init work
                return !((RawExecutionCode.INLINE_WORK && method.getName().startsWith("work")) || 
                        method.getName().startsWith("initWork"));
            }
        }).optimize((SIRFilter)node.contents);
        
        //convert pop() statements that aren't assigned to anything to be 
        //assigned to a dummy variable
        ConvertLonelyReceives.doit(toC.ssg, toC.flatNode);

        /*
          SIRPrinter printer1 = new SIRPrinter("sir" + node + ".out");
          IterFactory.createFactory().createIter(node.getFilter()).accept(printer1);
          printer1.close();
        */
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
        
        return buf.toString();
    }
    
    
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
        
        if (/*KjcOptions.altcodegen && */ 
                !(KjcOptions.decoupled || RawWorkEstimator.SIMULATING_WORK)){
            p.print(getNetRegsDecls());
            //print("unsigned " + DYNMSGHEADER + ";\n");
        }
        
    
        
        if (KjcOptions.decoupled || RawWorkEstimator.SIMULATING_WORK) {
            p.print("volatile float " + Util.CSTOFPVAR + ";\n");
            p.print("volatile float " + Util.CSTIFPVAR + ";\n");
            p.print("volatile int " + Util.CSTOINTVAR + ";\n");
            p.print("volatile int " + Util.CSTIINTVAR + ";\n");
            p.print("volatile float " + Util.CGNOFPVAR + ";\n");
            p.print("volatile float " + Util.CGNIFPVAR + ";\n");
            p.print("volatile int " + Util.CGNOINTVAR + ";\n");
            p.print("volatile int " + Util.CGNIINTVAR + ";\n");
        }
        
        if (SpaceDynamicBackend.FILTER_DEBUG_MODE) {
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
        
        
        if (RawWorkEstimator.SIMULATING_WORK) {
            if (RawWorkEstimator.structures != null && 
                    RawWorkEstimator.structures.length > 0)
                p.print("#include \"structs.h\"\n");    
        }
        else 
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
        

        //for the first filter/tile we encounter we are going to 
        //create a magic instruction that tells the number-gathering
        //stuff that everything is done snake booting, so if this is true
        if (!gen_magc_done_boot && KjcOptions.numbers > 0) {
            gen_magc_done_boot = true;
            p.print("  __asm__ volatile (\"magc $0, $0, 5\");\n");
        }
    
        
        //if we are using the dynamic network create the dynamic network
        //header to be used for all outgoing messages
        if (dynamicOutput) {
            FlatNode downstream = null;
            
            if (ssg.isOutput(flatNode)) { // the node is an endpoint of the ssg
                downstream = ssg.getStreamGraph().getParentSSG(flatNode).getNext(flatNode);
            } else {
                //the node is not an endpoint and we did not use a comm simulator
                //so everything is on the dynamic net
                if (flatNode.ways > 0) {
                    //it has to be a pipeline!
                    assert flatNode.ways == 1;
                    downstream = flatNode.getEdges()[0];
                }
            }
            
            if (downstream != null) {
                int size = Util.getTypeSize(ssg.getOutputType(flatNode));
                assert size < 31 : "Type size too large to fit in single dynamic network packet";
                /*System.out.println(flatNode + " " + layout.getTile(flatNode) + 
                  " dynamically sends to + " + downstream + " " + 
                  layout.getTile(downstream) + ", size = " + size);
                */
                //if the downstream filter is assigned to a tile then use the    
                //following raw helper function to construct the header for the push dynamic
                if (layout.isAssigned(downstream)) {          
                    p.print(" " + DYNMSGHEADER + " = construct_dyn_hdr(0, " +
                            size + ", 0, " +
                            (layout.getTile(self)).getY() + ", " +
                            (layout.getTile(self)).getX() + ", " + 
                            (layout.getTile(downstream)).getY() + "," +
                            (layout.getTile(downstream)).getX() + ");\n");
                }
                else {
                    //this filters outputs to a filewriter over the dynamic net 
                    //generate the header with the correct dir bit
                    genHeaderForDynFileWriter(downstream, size);
                }
            }
        }
        

        //if we are using the magic network, 
        //use a magic instruction to initialize the magic fifos
        if (KjcOptions.magic_net)
            p.print("  __asm__ volatile (\"magc $0, $0, 1\");\n");
        
        //initialize the dummy network receive value
        if (KjcOptions.decoupled || RawWorkEstimator.SIMULATING_WORK) {
            if (self.getInputType().isFloatingPoint()) 
                p.print("  " + Util.CSTIFPVAR + " = 1.0;\n");
            else 
                p.print("  " + Util.CSTIINTVAR + " = 1;\n");
        }

        //call the raw_init() function for the static network
        //only if we are not using a uniprocessor or the
        //magic network
        if (!(KjcOptions.standalone || KjcOptions.magic_net || KjcOptions.decoupled ||
              IMEMEstimation.TESTING_IMEM || RawWorkEstimator.SIMULATING_WORK)) {
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
    
    
    /**
     * Generate the dynamic network (GDN) network header for this filter; it 
     * outputs to a file writer over the dynamic network.  We have to set the 
     * final route field to route the words off the chip to the device.
     * 
     * @param self the flat node we are generating code for.
     * @param size The type size in words.
     * @param downstream The downstream file writer.
     */
    private void genHeaderForDynFileWriter(FlatNode downstream, int size) {
        assert downstream.contents instanceof SIRFileWriter : 
            "Didn't see an SIRFileWriter where one was expected, instead: " + downstream.contents; 
    
        FileWriterDevice fwd = ((SpdStreamGraph)ssg.getStreamGraph()).getFileState().getFileWriterDevice(downstream);
        //get the neighboring tile
        RawTile neighboringTile = fwd.getPort().getNeighboringTile();
        //now calculated the final route, once the packet gets to the destination (neighboring) tile
        //it will be routed off the chip by 
        // 2 = west, 3 = south, 4 = east, 5 = north
        int finalRoute = fwd.getPort().getDirectionFromTile();
                
        p.print(" " + DYNMSGHEADER + " = construct_dyn_hdr(" +
                finalRoute + ", " +
                size + ", " + 
                "0, " + //user
                (layout.getTile(flatNode)).getY() + ", " +
                (layout.getTile(flatNode)).getX() + ", " + 
                neighboringTile.getY() + "," +
                neighboringTile.getX() + ");\n");
        
    }   
        
    /** 
     * generate a function that will get the iteration count from the 
     * command-line, only generation this if --standalone is enabled...
     **/
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
        assert false;
    }

    private void createFile() {
        System.out.println("Code for " + filter.getShortIdent(30) +
                           " written to tile" + layout.getTileNumber(filter) +
                           ".c");
        try {
            FileWriter fw = new FileWriter("tile" + layout.getTileNumber(filter) + ".c");
            fw.write(p.getString().toString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write tile code file for filter " +
                               filter.getShortIdent(30));
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
        //prework or work function definition (if the work function is inlined)
        if (filter != null && 
            ((filter.getWork().equals(self) && RawExecutionCode.INLINE_WORK) ||
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
            else if (type.isArrayType()) { 
                p.print(" = {0}");
            }
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
            lastLeft = left;
            printLParen();      // parenthesize if expr not if stmt
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
            String dims[] = Util.makeString(type.getDims());

            //System.out.println("ArrayCopy: " + left + " = " + right);
            
            // dims should never be null now that we have static array
            // bounds
            assert dims != null;
            /*
            //if we cannot find the dim, just create a pointer copy
            if (dims == null) {
            lastLeft=left;
            printLParen();  // parenthesize if expr not if stmt
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

        lastLeft = left;
        printLParen();  // parenthesize if expr not if stmt
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

        /*  a hack that never was...
            if (ident.startsWith(ARRAY_INIT_PREFIX)) {
            assert args.length == 2 : "Error: improper args to init_array";
            assert args[0] instanceof JStringLiteral : "Error: improper args to init_array";
            assert args[1] instanceof JIntLiteral : "Error: improper args to init_array";
            
            p.print(getArrayInitFromFile(ident, 
            ((JStringLiteral)args[0]).stringValue(), 
            ((JIntLiteral)args[1]).intValue()));
            return;
            }
        */

        //generate the inline asm instruction to execute the 
        //receive if this is a receive instruction
        if (ident.equals(RawExecutionCode.receiveMethod)) {
            if (args.length > 0) {
                visitArgs(args,0);
                p.print(" = ");
                p.print(Util.networkReceive
                        (dynamicInput, CommonUtils.getBaseType(filter.getInputType())));
                
            }
            else {
                p.print(Util.networkReceive
                        (dynamicInput, CommonUtils.getBaseType(filter.getInputType())));
            }
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
        if (ident.startsWith(RawExecutionCode.structReceivePrefix))
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
        Utils.fail("We should not see a dynamic token in FlatIRToC");
    }

    public void visitRangeExpression(SIRRangeExpression self) {
        Utils.fail("We should not see a range expression in FlatIRToC");
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
        //handle the print statment using a magic instruction
        RawSimulatorPrint.visitPrintStatement(self, exp, p, this);
    } 
    
    private void pushScalar(SIRPushExpression self,
                            CType tapeType,
                            JExpression val) 
    {
        //      if (tapeType != val.getType()) {
        //    Utils.fail("type of push argument does not match filter output type");
        //      }
        

        //if this filter is the sink of a ssg, then we have to produce
        //dynamic network code!!!, check this by using some util methods.
        if (dynamicOutput) {
            p.print(Util.CGNOINTVAR + " = " + DYNMSGHEADER + ";");
            if (tapeType.isFloatingPoint())
                p.print(Util.CGNOFPVAR);
            else
                p.print(Util.CGNOINTVAR);
            p.print(" = (" + tapeType + ")");
            if (tapeType != val.getType())
                p.print("(" + tapeType + ")");
            val.accept(this);
            return;
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

    
    public void pushClass(SIRPushExpression self, 
                          CType tapeType,
                          JExpression val) 
    {
        //turn the push statement into a call of
        //the structure's push method

        p.print("push");
        
        if (dynamicOutput) 
            p.print("Dynamic");
        else
            p.print("Static");

        p.print(tapeType + "(&");
        val.accept(this);
        p.print(")");
    }
    

    private void pushArray(SIRPushExpression self, 
                           CType tapeType,
                           JExpression val) 
    {
        assert !dynamicOutput : "pushing of non-scalars at SSG boundary not supported yet";
        CType baseType = ((CArrayType)tapeType).getBaseType();
        String dims[] = Util.makeString(((CArrayType)tapeType).getDims());
        String ARRAY_INDEX = "ARRAY_PUSH_INDEX";

        //start a new block
        p.print("{\n");
        //print the decls of the loop indices
        p.print ("  int ");
        for (int i = 0; i < dims.length; i++) {
            p.print(ARRAY_INDEX + i);
            if (i < dims.length - 1)
                p.print(", ");
        }
        p.print(";\n");

        //print the for loops
        for (int i = 0; i < dims.length; i++) {
            p.print("for (" + ARRAY_INDEX + i + " = 0; " +
                    ARRAY_INDEX + i + " < " + dims[i] + " ; " +
                    ARRAY_INDEX + i + "++)\n");
        }

//        if(KjcOptions.altcodegen || KjcOptions.decoupled) {
            p.print("{\n");
            p.print(Util.networkSendPrefix(dynamicOutput, CommonUtils.getBaseType(tapeType)));
            val.accept(this);
            for (int i = 0; i < dims.length; i++) {
                p.print("[" + ARRAY_INDEX + i + "]");
            }
            p.print(Util.networkSendSuffix(dynamicOutput));
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

    private String getArrayInitFromFile(String method, String fileName, 
                                        int size) 
    {
        StringBuffer buf = new StringBuffer();
        String line;
        
        assert method.startsWith("init_array_");
        
        String dim = method.substring(11, 13);
        assert dim.equals("1D") : "Error: Only support for 1D array initialization from file " + dim;
        
        String type = method.substring(14);
        assert type.equals("int") || type.equals("float") : 
            "Error: unsupport type for array initialization from file";

        buf.append("{");
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            int index = 0;
            
            while ((line = in.readLine()) != null ){
                buf.append(line + ",\n");
                index ++;
                //break if we have read enough elements
                if (index == size)
                    break;
            }
            assert index == size : "Error: not enough elements in " + fileName +
                " to initialize array";
            //remove the trailing , (before the newline) and append the }
            buf.setCharAt(buf.length() - 2, '}');       
                       
            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error while opening/reading " + fileName +
                               " for array initialization");
            System.exit(1);
        }
        return buf.toString();
    }
    
}
