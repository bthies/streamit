package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
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

/**
 * This class dumps the tile code for each filter into a file based 
 * on the tile number assigned 
 */
public class FlatIRToC extends SLIREmptyVisitor implements StreamVisitor
{
    private boolean DEBUG = false;

    protected int				TAB_SIZE = 2;
    protected int				WIDTH = 80;
    protected int				pos;
    protected String                      className;

    protected TabbedPrintWriter		p;
    protected StringWriter                str; 
    protected boolean			nl = true;
    public boolean                   declOnly = true;
    public SIRFilter               filter;
    

    // ALWAYS!!!!
    //true if we are using the second buffer management scheme 
    //circular buffers with anding
    public boolean debug = false;//true;

    //true if we are currently visiting the init function
    private boolean isInit = false;
    
    //fields for all of the vars names we introduce in the c code
    private final String FLOAT_HEADER_WORD = "__FLOAT_HEADER_WORD__";
    private final String INT_HEADER_WORD = "__INT_HEADER_WORD__";


    private static int filterID = 0;
    
    //Needed to pass info from assignment to visitNewArray
    JExpression lastLeft;

    public static void generateCode(FlatNode node) 
    {
	FlatIRToC toC = new FlatIRToC((SIRFilter)node.contents);
	//FieldInitMover.moveStreamInitialAssignments((SIRFilter)node.contents);
	//FieldProp.doPropagate((SIRFilter)node.contents);

	//Optimizations
	
	
	if(!KjcOptions.nofieldprop)
	    System.out.println
		("Optimizing "+
		 ((SIRFilter)node.contents).getName()+"...");

	ArrayDestroyer arrayDest=new ArrayDestroyer();
	for (int i = 0; i < ((SIRFilter)node.contents).getMethods().length; i++) {
	    JMethodDeclaration method=((SIRFilter)node.contents).getMethods()[i];
	    
	    if(!(method.getName().startsWith("work")||method.getName().startsWith("initWork"))) { 
		//Already in __RAWMAIN__
		if (!KjcOptions.nofieldprop) {
		
		    Unroller unroller;
		    do {
			do {
			    //System.out.println("Unrolling..");
			    unroller = new Unroller(new Hashtable());
			    method.accept(unroller);
			} while(unroller.hasUnrolled());
			//System.out.println("Constant Propagating..");
			method.accept(new Propagator(new Hashtable()));
			//System.out.println("Unrolling..");
			unroller = new Unroller(new Hashtable());
			method.accept(unroller);
		    } while(unroller.hasUnrolled());
		    //System.out.println("Flattening..");
		    method.accept(new BlockFlattener());
		    //System.out.println("Analyzing Branches..");
		    //method.accept(new BranchAnalyzer());
		    //System.out.println("Constant Propagating..");
		    method.accept(new Propagator(new Hashtable()));
		} else
		    method.accept(new BlockFlattener());
		method.accept(arrayDest);
		method.accept(new VarDeclRaiser());
	    }
	}
	if(KjcOptions.destroyfieldarray)
	   arrayDest.destroyFieldArrays((SIRFilter)node.contents);
	   /*	
	  try {
	    SIRPrinter printer1 = new SIRPrinter();
	    IterFactory.createFactory().createIter((SIRFilter)node.contents).accept(printer1);
	    printer1.close();
	}
	catch (Exception e) 
	    {
	    }
	*/
        IterFactory.createFactory().createIter((SIRFilter)node.contents).accept(toC);
    }
    
    public FlatIRToC() 
    {
	this.str = new StringWriter();
        this.p = new TabbedPrintWriter(str);
    }
    

    public FlatIRToC(TabbedPrintWriter p) {
        this.p = p;
        this.str = null;
        this.pos = 0;
    }
    
    public FlatIRToC(SIRFilter f) {
	this.filter = f;
	//	circular = false;
	this.str = new StringWriter();
        this.p = new TabbedPrintWriter(str);
    }

    public String getString() {
        if (str != null)
            return str.toString();
        else
            return null;
    }
    
    /**
     * Close the stream at the end
     */
    public void close() {
        p.close();
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    /*  
    public void visitStructure(SIRStructure self,
                               SIRStream parent,
                               JFieldDeclaration[] fields)
    {
        print("struct " + self.getIdent() + " {\n");
        for (int i = 0; i < fields.length; i++)
            fields[i].accept(this);
        print("};\n");
    }
    */
    
    public void visitFilter(SIRFilter self,
			    SIRFilterIter iter) {

	//       System.out.println(self.getName());
	
	//Entry point of the visitor

	//do not print the raw header if compiling
	//for uniprocessor
	if (!KjcOptions.standalone) 
	    print("#include <raw.h>\n");
	print("#include <stdlib.h>\n");
	print("#include <math.h>\n\n");

	//if we are number gathering and this is the sink, generate the dummy
	//vars for the assignment of the print expression.
	if (KjcOptions.numbers > 0 && NumberGathering.successful) {
	    print ("volatile int dummyInt;\n");
	    print ("volatile float dummyFloat;\n");
	}
	
	if (KjcOptions.altcodegen && !KjcOptions.decoupled){
	    print("register float " + Util.CSTOFPVAR + " asm(\"$csto\");\n");
	    print("register float " + Util.CSTIFPVAR + " asm(\"$csti\");\n");
	    print("register int " + Util.CSTOINTVAR + " asm(\"$csto\");\n");
	    print("register int " + Util.CSTIINTVAR + " asm(\"$csti\");\n");
	}
	
	if (KjcOptions.decoupled) {
	    print("volatile float " + Util.CSTOFPVAR + ";\n");
	    print("volatile float " + Util.CSTIFPVAR + ";\n");
	    print("volatile int " + Util.CSTOINTVAR + ";\n");
	    print("volatile int " + Util.CSTIINTVAR + ";\n");
	}
	
	if (RawBackend.FILTER_DEBUG_MODE) {
	    print("void static_send_print(");
	    if (self.getOutputType() == CStdType.Void)
		print("int f) {\n");		
	    else 
		print(self.getOutputType() + " f) {\n");
	    if (self.getOutputType().isFloatingPoint()) 
		print("print_float(f);\n");
	    else 
		print("print_int(f);\n");
	    print("static_send(f);\n");
	    print("}\n\n");
	}
	
	//if there are structures in the code, include
	//the structure definition header files
	if (RawBackend.structures.length > 0) 
	    print("#include \"structs.h\"\n");

	//print the extern for the function to init the 
	//switch, do not do this if we are compiling for
	//a uniprocessor
	if (!KjcOptions.standalone) {
	    print("void raw_init();\n");
	    print("void raw_init2();\n");
	}


		
	//not used any more
	//print("unsigned int " + FLOAT_HEADER_WORD + ";\n");
	//print("unsigned int " + INT_HEADER_WORD + ";\n");
       
	//Visit fields declared in the filter class
	JFieldDeclaration[] fields = self.getFields();
	for (int i = 0; i < fields.length; i++)
	   fields[i].accept(this);
	
	//visit methods of filter, print the declaration first
	declOnly = true;
	JMethodDeclaration[] methods = self.getMethods();
	for (int i =0; i < methods.length; i++)
	    methods[i].accept(this);
	//now print the functions with body
	declOnly = false;
	for (int i =0; i < methods.length; i++) {
	    methods[i].accept(this);	
	}
	
	//if we are generating raw code print the begin
	//method for the simulator
	if (!KjcOptions.standalone) {
	    print("void begin(void) {\n");
	}
	else {
	    //otherwise print a normal main()
	    print("int main() {\n");
	}
	//not used at this time
	//print(FLOAT_HEADER_WORD + 
	//" = construct_dyn_hdr(3, 1, 0, 0, 0, 3, 0);\n");
	//print(INT_HEADER_WORD + 
	//" = construct_dyn_hdr(3, 1, 1, 0, 0, 3, 0);\n");
	
	//if we are using the magic network, 
	//use a magic instruction to initialize the magic fifos
	if (KjcOptions.magic_net)
	    print("  __asm__ volatile (\"magc $0, $0, 1\");\n");
	
	//initialize the dummy network receive value
	if (KjcOptions.decoupled) {
	    if (self.getInputType().isFloatingPoint()) 
		print("  " + Util.CSTIFPVAR + " = 1.0;\n");
	    else 
		print("  " + Util.CSTIINTVAR + " = 1;\n");
	}

	//call the raw_init() function for the static network
	//only if we are not using a uniprocessor or the
	//magic network
	if (!KjcOptions.standalone && !KjcOptions.magic_net && !KjcOptions.decoupled) {
	    print("  raw_init();\n");
	    print("  raw_init2();\n");
	}
	//execute the raw main function
	print(RawExecutionCode.rawMain + "();\n");
	//return 0 if we are generating normal c code
	if (KjcOptions.standalone) 
	    print("  return 0;\n");
	print("}\n");
       
	createFile();
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
	    fw.write(str.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Unable to write tile code file for filter " +
			       filter.getName());
	}
    }
       			
    /**
     * prints a field declaration
     */
    public void visitFieldDeclaration(JFieldDeclaration self,
                                      int modifiers,
                                      CType type,
                                      String ident,
                                      JExpression expr) {
        /*
          if (ident.indexOf("$") != -1) {
          return; // dont print generated elements
          }
        */

        newLine();
        // print(CModifier.toString(modifiers));

	//only stack allocate singe dimension arrays
	if (expr instanceof JNewArrayExpression) {
	    //print the basetype
	    print(((CArrayType)type).getBaseType() + " ");
	    //print the field identifier
	    print(ident);
	    //print the dims
	    stackAllocateArray(ident);
	    print(";");
	    return;
	}


        print(type);
        print(" ");
        print(ident);

        if (expr != null) {
            print("\t= ");
	    expr.accept(this);
        }   //initialize all fields to 0
	else if (type.isOrdinal())
	    print (" = 0");
	else if (type.isFloatingPoint())
	    print(" = 0.0f");

        print(";");
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

	
	
	   
        newLine();
	// print(CModifier.toString(modifiers));
	print(returnType);
	print(" ");
	//just print initPath() instead of initPath<Type>
	if (ident.startsWith("initPath"))
	    print("initPath"); 
	else 
	    print(ident);
	print("(");
	int count = 0;
	
	for (int i = 0; i < parameters.length; i++) {
	    if (count != 0) {
		print(", ");
	    }
	    parameters[i].accept(this);
	    count++;
	}
	print(")");
	
	//print the declaration then return
	if (declOnly) {
	    print(";");
	    return;
	}
	
	//set is init for dynamically allocating arrays...
	if (filter != null &&
	    self.getName().startsWith("init"))
	    isInit = true;
	

        print(" ");
        if (body != null) 
	    body.accept(this);
        else 
            print(";");

        newLine();
	isInit = false;
    }

    private void dummyWork(int push) {
	print("{\n");
	print("  int i;\n");
	print("  for(i = 0; i < " + push + "; i++)\n");
	print("    static_send(i);\n");
	print("}\n");
    }

    private int nextPow2(int i) {
	String str = Integer.toBinaryString(i);
	if  (str.indexOf('1') == -1)
	    return 0;
	int bit = str.length() - str.indexOf('1');
	return (int)Math.pow(2, bit);
    }
    
    // ----------------------------------------------------------------------
    // STATEMENT
    // ----------------------------------------------------------------------

    /**
     * prints a while statement
     */
    public void visitWhileStatement(JWhileStatement self,
                                    JExpression cond,
                                    JStatement body) {
        print("while (");
        cond.accept(this);
        print(") ");

        body.accept(this);
    }

    /**
     * prints a variable declaration statement
     */
    public void visitVariableeclarationStatement(JVariableDeclarationStatement self,
                                                  JVariableDefinition[] vars) {
        for (int i = 0; i < vars.length; i++) {
            vars[i].accept(this);
        }
    }

    private void printLocalArrayDecl(JNewArrayExpression expr) 
    {
	JExpression[] dims = expr.getDims();
	for (int i = 0 ; i < dims.length; i++) {
	    FlatIRToC toC = new FlatIRToC();
	    dims[i].accept(toC);
	    print("[" + toC.getString() + "]");
	}
    }
    

    /**
     * prints a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {

        // print(CModifier.toString(modifiers));
	//	System.out.println(ident);
	//System.out.println(expr);

	//we want to stack allocate all arrays not in the init
	//we convert an assignment statement into the stack allocation statement'
	//so, just remove the var definition, if the new array expression
	//is not included in this definition, just remove the definition,
	//when we visit the new array expression we will print the definition...
	if (type.isArrayType() && !isInit) {
	    String[] dims = ArrayDim.findDim(filter, ident);
	    //but only do this if the array has corresponding 
	    //new expression, otherwise don't print anything.
	    if (expr instanceof JNewArrayExpression) {
		//print the type
		print(((CArrayType)type).getBaseType() + " ");
		//print the field identifier
		print(ident);
		//print the dims
		stackAllocateArray(ident);
		print(";");
		return;
	    }
	    else if (dims != null)
		return;
	}
	
	if (expr!=null) {
	    printLocalType(type);
	} else {
	    print(type);
	}	    
        print(" ");
	print(ident);
        if (expr != null) {
	    print(" = ");
	    expr.accept(this);
	}
	else if (type.isOrdinal())
	    print (" = 0");
	else if (type.isFloatingPoint())
	    print(" = 0.0f");

        print(";\n");

    }


    /**
     * prints a switch statement
     */
    public void visitSwitchStatement(JSwitchStatement self,
                                     JExpression expr,
                                     JSwitchGroup[] body) {
        print("switch (");
        expr.accept(this);
        print(") {");
        for (int i = 0; i < body.length; i++) {
            body[i].accept(this);
        }
        newLine();
        print("}");
    }

    /**
     * prints a return statement
     */
    public void visitReturnStatement(JReturnStatement self,
                                     JExpression expr) {
        print("return");
        if (expr != null) {
            print(" ");
            expr.accept(this);
        }
        print(";");
    }

    /**
     * prints a labeled statement
     */
    public void visitLabeledStatement(JLabeledStatement self,
                                      String label,
                                      JStatement stmt) {
        print(label + ":");
        stmt.accept(this);
    }

    /**
     * prints a if statement
     */
    public void visitIfStatement(JIfStatement self,
                                 JExpression cond,
                                 JStatement thenClause,
                                 JStatement elseClause) {
        print("if (");
        cond.accept(this);
        print(") {");
        pos += thenClause instanceof JBlock ? 0 : TAB_SIZE;
        thenClause.accept(this);
        pos -= thenClause instanceof JBlock ? 0 : TAB_SIZE;
        if (elseClause != null) {
            if ((elseClause instanceof JBlock) || (elseClause instanceof JIfStatement)) {
                print(" ");
            } else {
                newLine();
            }
            print("} else {");
            pos += elseClause instanceof JBlock || elseClause instanceof JIfStatement ? 0 : TAB_SIZE;
            elseClause.accept(this);
            pos -= elseClause instanceof JBlock || elseClause instanceof JIfStatement ? 0 : TAB_SIZE;
        }
	print("}");
    }

    /**
     * prints a for statement
     */
    public void visitForStatement(JForStatement self,
                                  JStatement init,
                                  JExpression cond,
                                  JStatement incr,
                                  JStatement body) {
        print("for (");
        if (init != null) {
            init.accept(this);
	    //the ; will print in a statement visitor
	}

        print(" ");
        if (cond != null) {
            cond.accept(this);
        }
	//cond is an expression so print the ;
        print("; ");
	if (incr != null) {
	    FlatIRToC l2c = new FlatIRToC(filter);
            incr.accept(l2c);
	    // get String
	    String str = l2c.getString();
	    // leave off the trailing semicolon if there is one
	    if (str.endsWith(";")) {
		print(str.substring(0, str.length()-1));
	    } else { 
		print(str);
	    }
        }

        print(") ");

        print("{");
        pos += TAB_SIZE;
        body.accept(this);
        pos -= TAB_SIZE;
        newLine();
        print("}");
    }

    /**
     * prints a compound statement
     */
    public void visitCompoundStatement(JCompoundStatement self,
                                       JStatement[] body) {
        visitCompoundStatement(body);
    }

    /**
     * prints a compound statement
     */
    public void visitCompoundStatement(JStatement[] body) {
        for (int i = 0; i < body.length; i++) {
            if (body[i] instanceof JIfStatement &&
                i < body.length - 1 &&
                !(body[i + 1] instanceof JReturnStatement)) {
                newLine();
            }
            if (body[i] instanceof JReturnStatement && i > 0) {
                newLine();
            }

            newLine();
            body[i].accept(this);

            if (body[i] instanceof JVariableDeclarationStatement &&
                i < body.length - 1 &&
                !(body[i + 1] instanceof JVariableDeclarationStatement)) {
                newLine();
            }
        }
    }

    /**
     * prints an expression statement
     */
    public void visitExpressionStatement(JExpressionStatement self,
                                         JExpression expr) {
        expr.accept(this);
	print(";");
    }

    /**
     * prints an expression list statement
     */
    public void visitExpressionListStatement(JExpressionListStatement self,
                                             JExpression[] expr) {
        for (int i = 0; i < expr.length; i++) {
            if (i != 0) {
                print(", ");
            }
            expr[i].accept(this);
        }
	print(";");
    }

    /**
     * prints a empty statement
     */
    public void visitEmptyStatement(JEmptyStatement self) {
        newLine();
        print(";");
    }

    /**
     * prints a do statement
     */
    public void visitDoStatement(JDoStatement self,
                                 JExpression cond,
                                 JStatement body) {
        newLine();
        print("do ");
        body.accept(this);
        print("");
        print("while (");
        cond.accept(this);
        print(");");
    }

    /**
     * prints a continue statement
     */
    public void visitContinueStatement(JContinueStatement self,
                                       String label) {
        newLine();
        print("continue");
        if (label != null) {
            print(" " + label);
        }
        print(";");
    }

    /**
     * prints a break statement
     */
    public void visitBreakStatement(JBreakStatement self,
                                    String label) {
        newLine();
        print("break");
        if (label != null) {
            print(" " + label);
        }
        print(";");
    }

    /**
     * prints an expression statement
     */
    public void visitBlockStatement(JBlock self,
                                    JavaStyleComment[] comments) {
        print("{");
        pos += TAB_SIZE;
        visitCompoundStatement(self.getStatementArray());
        if (comments != null) {
            visitComments(comments);
        }
        pos -= TAB_SIZE;
        newLine();
        print("}");
    }

    /**
     * prints a type declaration statement
     */
    public void visitTypeDeclarationStatement(JTypeDeclarationStatement self,
                                              JTypeDeclaration decl) {
        decl.accept(this);
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * prints an unary plus expression
     */
    public void visitUnaryPlusExpression(JUnaryExpression self,
                                         JExpression expr)
    {
	print("(");
        print("+");
        expr.accept(this);
	print(")");
    }

    /**
     * prints an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
                                          JExpression expr)
    {
	print("(");
        print("-");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	print("(");
        print("~");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	print("(");
        print("!");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a type name expression
     */
    public void visitTypeNameExpression(JTypeNameExpression self,
                                        CType type) {
	print("(");
        print(type);
	print(")");
    }

    /**
     * prints a this expression
     */
    public void visitThisExpression(JThisExpression self,
                                    JExpression prefix) {
	//Utils.fail("This Expression encountered");
    }

    /**
     * prints a super expression
     */
    public void visitSuperExpression(JSuperExpression self) {
        Utils.fail("Super Expression Encountered");
    }

    /**
     * prints a shift expression
     */
    public void visitShiftExpression(JShiftExpression self,
                                     int oper,
                                     JExpression left,
                                     JExpression right) {
	print("(");
        left.accept(this);
        if (oper == OPE_SL) {
            print(" << ");
        } else if (oper == OPE_SR) {
            print(" >> ");
        } else {
            print(" >>> ");
        }
        right.accept(this);
	print(")");
    }

    /**
     * prints a shift expressiona
     */
    public void visitRelationalExpression(JRelationalExpression self,
                                          int oper,
                                          JExpression left,
                                          JExpression right) {
	print("(");
        left.accept(this);
        switch (oper) {
        case OPE_LT:
            print(" < ");
            break;
        case OPE_LE:
            print(" <= ");
            break;
        case OPE_GT:
            print(" > ");
            break;
        case OPE_GE:
            print(" >= ");
            break;
        default:
            Utils.fail("Unknown relational expression");
	}
        right.accept(this);
	print(")");
    }

    /**
     * prints a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr) {
	print("(");
        if (oper == OPE_PREINC) {
            print("++");
        } else {
            print("--");
        }
        expr.accept(this);
	print(")");
    }

    /**
     * prints a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr) {
	print("(");
        expr.accept(this);
        if (oper == OPE_POSTINC) {
            print("++");
        } else {
            print("--");
        }
	print(")");
    }

    /**
     * prints a parenthesed expression
     */
    public void visitParenthesedExpression(JParenthesedExpression self,
                                           JExpression expr) {
        print("(");
        expr.accept(this);
        print(")");
    }



    /**
     * prints an array allocator expression
     */
    public void visitNewArrayExpression(JNewArrayExpression self,
                                        CType type,
                                        JExpression[] dims,
                                        JArrayInitializer init)
    {
        /*print("(" + type + "*) calloc(");
	  dims[0].accept(this);
	  print(" , sizeof(");
	  print(type);
	  print("))");
	  if (init != null) {
	  init.accept(this);
	  }*/
	print("calloc(");
        dims[0].accept(this);
        print(", sizeof(");
        print(type);
	if(dims.length>1)
	    print("*");
        print("))");
	if(dims.length>1) {
	    for(int off=0;off<(dims.length-1);off++) {
		//Right now only handles JIntLiteral dims
		//If cast expression then probably a failure to reduce
		int num=((JIntLiteral)dims[off]).intValue();
		for(int i=0;i<num;i++) {
		    print(",\n");
		    //If lastLeft null then didn't come right after an assignment
		    lastLeft.accept(this);
		    print("["+i+"]=calloc(");
		    dims[off+1].accept(this);
		    print(", sizeof(");
		    print(type);
		    if(off<(dims.length-2))
			print("*");
		    print("))");
		}
	    }
	}
        if (init != null) {
            init.accept(this);
        }
    }

    /**
     * prints a name expression
     */
    public void visitNameExpression(JNameExpression self,
                                    JExpression prefix,
                                    String ident) {
	Utils.fail("Name Expression");
	
	print("(");
        if (prefix != null) {
            prefix.accept(this);
            print("->");
        }
        print(ident);
	print(")");
    }

    /**
     * prints an array allocator expression
     */
    public void visitBinaryExpression(JBinaryExpression self,
                                      String oper,
                                      JExpression left,
                                      JExpression right) {
	print("(");
        left.accept(this);
        print(" ");
        print(oper);
        print(" ");
        right.accept(this);
	print(")");
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
	    print("for (" + RawExecutionCode.ARRAY_INDEX + i + " = 0; " +
		  RawExecutionCode.ARRAY_INDEX + i + " < " + dims[i] + " ; " +
		  RawExecutionCode.ARRAY_INDEX + i + "++)\n");
	}
	
	print("{");
	//print out the receive assembly
	print(Util.staticNetworkReceivePrefix());
	//print out the buffer variable and the index
	arg.accept(this);
	//now append the remaining dimensions
	for (int i = 0; i < dims.length; i++) {
		print("[" + RawExecutionCode.ARRAY_INDEX + i + "]");
	    }
	//finish up the receive assembly
	print(Util.staticNetworkReceiveSuffix(((CArrayType)filter.getInputType()).getBaseType()));
	print("}");
    }
    */
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
	//	return;
	//}

	//generate the inline asm instruction to execute the 
	//receive if this is a receive instruction
	if (ident.equals(RawExecutionCode.receiveMethod)) {
	    print(Util.staticNetworkReceivePrefix());
	    visitArgs(args,0);
	    print(Util.staticNetworkReceiveSuffix
		  (Util.getBaseType(filter.getInputType())));
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
        print(ident);
	
	//we want single precision versions of the math functions
	if (isMathMethod(prefix, ident)) 
	    print("f");
	    
	print("(");
	
	//if this method we are calling is the call to a structure 
	//receive method that takes a pointer, we have to add the 
	//address of operator
	if (ident.startsWith(RawExecutionCode.structReceiveMethodPrefix))
	    print("&");

        int i = 0;
        /* Ignore prefix, since it's just going to be a Java class name.
        if (prefix != null) {
            prefix.accept(this);
            i++;
        }
        */
        visitArgs(args, i);
        print(")");
    }

    private boolean isMathMethod(JExpression prefix, String ident) 
    {
	if (prefix instanceof JTypeNameExpression &&
	    ((JTypeNameExpression)prefix).getQualifiedName().equals("java/lang/Math") &&
	   
	    (ident.equals("acos") ||
	     ident.equals("asin") ||
	     ident.equals("atan") ||
	     ident.equals("atan2") ||
	     ident.equals("ceil") ||
	     ident.equals("cos") ||
	     ident.equals("sin") ||
	     ident.equals("cosh") ||
	     ident.equals("sinh") ||
	     ident.equals("exp") ||
	     ident.equals("fabs") ||
	     ident.equals("modf") ||
	     ident.equals("fmod") ||
	     ident.equals("frexp") ||
	     ident.equals("floor") ||	     
	     ident.equals("log") ||
	     ident.equals("log10") ||
	     ident.equals("pow") ||
	     ident.equals("rint") ||
	     ident.equals("sqrt") ||
	     ident.equals("tanh") ||
	     ident.equals("tan")))
	    return true;
	return false;
    }
    

    /**
     * prints a local variable expression
     */
    public void visitLocalVariableExpression(JLocalVariableExpression self,
                                             String ident) {
        print(ident);
    }

    /**
     * prints an equality expression
     */
    public void visitEqualityExpression(JEqualityExpression self,
                                        boolean equal,
                                        JExpression left,
                                        JExpression right) {
	print("(");
        left.accept(this);
        print(equal ? " == " : " != ");
        right.accept(this);
	print(")");
    }

    /**
     * prints a conditional expression
     */
    public void visitConditionalExpression(JConditionalExpression self,
                                           JExpression cond,
                                           JExpression left,
                                           JExpression right) {
	print("(");
        cond.accept(this);
        print(" ? ");
        left.accept(this);
        print(" : ");
        right.accept(this);
	print(")");
    }

    /**
     * prints a compound expression
     */
    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                                  int oper,
                                                  JExpression left,
                                                  JExpression right) {
	print("(");
        left.accept(this);
        switch (oper) {
        case OPE_STAR:
            print(" *= ");
            break;
        case OPE_SLASH:
            print(" /= ");
            break;
        case OPE_PERCENT:
            print(" %= ");
            break;
        case OPE_PLUS:
            print(" += ");
            break;
        case OPE_MINUS:
            print(" -= ");
            break;
        case OPE_SL:
            print(" <<= ");
            break;
        case OPE_SR:
            print(" >>= ");
            break;
        case OPE_BSR:
            print(" >>>= ");
            break;
        case OPE_BAND:
            print(" &= ");
            break;
        case OPE_BXOR:
            print(" ^= ");
            break;
        case OPE_BOR:
            print(" |= ");
            break;
        }
        right.accept(this);
	print(")");
    }

    /**
     * prints a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
                                     JExpression left,
                                     String ident)
    {
        if (ident.equals(JAV_OUTER_THIS)) {// don't generate generated fields
            print(left.getType().getCClass().getOwner().getType() + "->this");
            return;
        }
        int		index = ident.indexOf("_$");
        if (index != -1) {
            print(ident.substring(0, index));      // local var
        } else {
	    print("(");
            left.accept(this);
	    if (!(left instanceof JThisExpression))
		print(".");
            print(ident);
	    print(")");
        }
    }

    /**
     * prints a cast expression
     */
    public void visitCastExpression(JCastExpression self,
				    JExpression expr,
				    CType type)
    {
	print("(");
        print("(");
        print(type);
        print(")");
        print("(");
	expr.accept(this);
	print(")");
        print(")");
    }
    
    /**
     * prints a cast expression
     */
    public void visitUnaryPromoteExpression(JUnaryPromote self,
                                            JExpression expr,
                                            CType type)
    {
	print("(");
        print("(");
        print(type);
        print(")");
        print("(");
        expr.accept(this);
        print(")");
        print(")");
    }

    /**
     * prints a compound assignment expression
     */
    public void visitBitwiseExpression(JBitwiseExpression self,
                                       int oper,
                                       JExpression left,
                                       JExpression right) {
        print("(");
        left.accept(this);
        switch (oper) {
        case OPE_BAND:
            print(" & ");
            break;
        case OPE_BOR:
            print(" | ");
            break;
        case OPE_BXOR:
            print(" ^ ");
            break;
        default:
	    Utils.fail("Unknown relational expression");
        }
        right.accept(this);
        print(")");
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
	    print("(");
	    left.accept(this);
	    print(" = ");
	    right.accept(this);
	    print(")");
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
	    
	    String[] dims = ArrayDim.findDim(filter, ident);
	    //if we cannot find the dim, just create a pointer copy
	    if (dims == null) {
		lastLeft=left;
		print("(");
		left.accept(this);
		print(" = ");
		right.accept(this);
		print(")");
		return;
	    }
	    print("{\n");
	    print("int ");
	    //print the index var decls
	    for (int i = 0; i < dims.length -1; i++)
		print(RawExecutionCode.ARRAY_COPY + i + ", ");
	    print(RawExecutionCode.ARRAY_COPY + (dims.length - 1));
	    print(";\n");
	    for (int i = 0; i < dims.length; i++) {
		print("for (" + RawExecutionCode.ARRAY_COPY + i + " = 0; " + RawExecutionCode.ARRAY_COPY + i +  
		      " < " + dims[i] + "; " + RawExecutionCode.ARRAY_COPY + i + "++)\n");
	    }
	    left.accept(this);
	    for (int i = 0; i < dims.length; i++)
		print("[" + RawExecutionCode.ARRAY_COPY + i + "]");
	    print(" = ");
	    right.accept(this);
	    for (int i = 0; i < dims.length; i++)
		print("[" + RawExecutionCode.ARRAY_COPY + i + "]");
	    print(";\n}\n");
	    return;
	}

	//stack allocate all arrays when not in init function
	//done at the variable definition
	if (right instanceof JNewArrayExpression &&
 	    (left instanceof JLocalVariableExpression) && !isInit) {
	    //	    (((CArrayType)((JNewArrayExpression)right).getType()).getArrayBound() < 2)) {

	    //get the basetype and print it 
	    CType baseType = ((CArrayType)((JNewArrayExpression)right).getType()).getBaseType();
	    print(baseType + " ");
	    //print the identifier
	    
	    //Before the ISCA hack this was how we printed the var ident
	    //	    left.accept(this);
	    
	    String ident;
	    ident = ((JLocalVariableExpression)left).getVariable().getIdent();


	    
	    if (KjcOptions.ptraccess) {
		
		//HACK FOR THE ICSA PAPER, !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		//turn all array access into access of a pointer pointing to the array
		print(ident + "_Alloc");
		
		//print the dims of the array
		stackAllocateArray(ident);

		//print the pointer def and the assignment to the array
		print(";\n");
		print(baseType + " *" + ident + " = " + ident + "_Alloc");
	    }
	    else {
		//the way it used to be before the hack
		 left.accept(this);
		 //print the dims of the array
		 stackAllocateArray(ident);
	    }
	    return;
	}
           

	
	lastLeft=left;
        print("(");
        left.accept(this);
        print(" = ");
        right.accept(this);
        print(")");
    }

    //stack allocate the array
    private void stackAllocateArray(String ident) {
	//find the dimensions of the array!!
	String dims[] = 
	    ArrayDim.findDim(filter, ident);
	
	for (int i = 0; i < dims.length; i++)
	    print("[" + dims[i] + "]");
	return;
    }
    
    /**
     * prints an array length expression
     */
    public void visitArrayLengthExpression(JArrayLengthExpression self,
                                           JExpression prefix) {
	Utils.fail("Array length expression not supported in streamit");
	
        prefix.accept(this);
        print(".length");
    }

    public JExpression passParentheses(JExpression exp) 
    {
	while (exp instanceof JParenthesedExpression)
	    exp = ((JParenthesedExpression)exp).getExpr();
	
	return exp;
    }
    

    /**
     * prints an array length expression
     */
    public void visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor) {
        print("(");
        prefix.accept(this);
        print("[(int)");
        accessor.accept(this);
        print("]");
        print(")");
    }


    // ----------------------------------------------------------------------
    // STREAMIT IR HANDLERS
    // ----------------------------------------------------------------------

    public void visitCreatePortalExpression(SIRCreatePortal self) {
        print("create_portal()");
    }

    public void visitInitStatement(SIRInitStatement self,
                                   SIRStream stream)
    {
        print("/* InitStatement */");
    }

    public void visitInterfaceTable(SIRInterfaceTable self)
    {
        String iname = self.getIface().getIdent();
        JMethodDeclaration[] methods = self.getMethods();
        boolean first = true;
        
        print("{ ");
        for (int i = 0; i < methods.length; i++)
        {
            if (!first) print(", ");
            first = false;
            print(iname + "_" + methods[i].getName());
        }
        print("}");
    }
    
    public void visitLatency(SIRLatency self)
    {
        print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencyMax(SIRLatencyMax self)
    {
        print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencyRange(SIRLatencyRange self)
    {
        print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencySet(SIRLatencySet self)
    {
        print("LATENCY_BEST_EFFORT");
    }

    public void visitMessageStatement(SIRMessageStatement self,
                                      JExpression portal,
                                      String iname,
                                      String ident,
                                      JExpression[] params,
                                      SIRLatency latency)
    {
	print("send_" + iname + "_" + ident + "(");
        portal.accept(this);
        print(", ");
        latency.accept(this);
        if (params != null)
            for (int i = 0; i < params.length; i++)
                if (params[i] != null)
                {
                    print(", ");
                    params[i].accept(this);
                }
        print(");");
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
	if (KjcOptions.numbers > 0 && NumberGathering.successful) {
	    //assign the expression to a dummy var do it does not get
	    //optimized out...
	    print("dummy");
	    if (type.isFloatingPoint())
		print("Float");
	    else 
		print("Int");
	    print(" = ");
	    exp.accept(this);
	    print(";\n");
	    print("__asm__ volatile (\"magc $0, $0, 2\");\n");
	    return;
	}

	    
	if (type.equals(CStdType.Boolean))
	    {
		Utils.fail("Cannot print a boolean");
	    }
	else if (type.equals(CStdType.Byte) ||
		 type.equals(CStdType.Integer) ||
		 type.equals(CStdType.Short))
	    {
		if (!KjcOptions.standalone)
		    print("print_int(");
		else
		    print("printf(\"%d\\n\", "); 
		//print("gdn_send(" + INT_HEADER_WORD + ");\n");
		//print("gdn_send(");
		exp.accept(this);
		print(");");
	    }
	else if (type.equals(CStdType.Char))
	    {
		if (!KjcOptions.standalone)
		    print("print_int(");
		else
		    print("printf(\"%d\\n\", "); 
		//print("gdn_send(" + INT_HEADER_WORD + ");\n");
		//print("gdn_send(");
		exp.accept(this);
		print(");");
	    }
	else if (type.equals(CStdType.Float))
	    {
		if (!KjcOptions.standalone)
		    print("print_float(");
		else 
		    print("printf(\"%f\\n\", "); 
		//print("gdn_send(" + FLOAT_HEADER_WORD + ");\n");
		//print("gdn_send(");
		exp.accept(this);
		print(");");
	    }
        else if (type.equals(CStdType.Long))
	    {
		if (!KjcOptions.standalone)
		    print("print_int(");
		else
		    print("printf(\"%d\\n\", "); 
		//		print("gdn_send(" + INT_HEADER_WORD + ");\n");
		//print("gdn_send(");
		exp.accept(this);
		print(");");
	    }
	else if (type.equals(CStdType.String)) 
	    {
		if (!KjcOptions.standalone)
		    print("print_string(");
		else
		    print("printf(\"%s\\n\", "); 
		//		print("gdn_send(" + INT_HEADER_WORD + ");\n");
		//print("gdn_send(");
		exp.accept(this);
		print(");");
	    }
	else
	    {
		System.out.println("Unprintatble type");
		print("print_int(");
		exp.accept(this);
		print(");");
		//Utils.fail("Unprintable Type");
	    }
    }
    
    private void pushScalar(SIRPushExpression self,
			    CType tapeType,
			    JExpression val) 
    {
	//	if (tapeType != val.getType()) {
	//    Utils.fail("type of push argument does not match filter output type");
	//	}
	print(Util.staticNetworkSendPrefix(tapeType));
	//if the type of the argument to the push statement does not 
	//match the filter output type, print a cast.
	if (tapeType != val.getType())
	    print("(" + tapeType + ")");
	val.accept(this);
	print(Util.staticNetworkSendSuffix());
    }

    
    public void pushClass(SIRPushExpression self, 
			  CType tapeType,
			  JExpression val) 
    {
	//turn the push statement into a call of
	//the structure's push method
	print("push" + tapeType + "(&");
	val.accept(this);
	print(")");
    }
    

    private void pushArray(SIRPushExpression self, 
			   CType tapeType,
			   JExpression val) 
    {
	CType baseType = ((CArrayType)tapeType).getBaseType();
	String dims[] = Util.makeString(((CArrayType)tapeType).getDims());
	
	for (int i = 0; i < dims.length; i++) {
	    print("for (" + RawExecutionCode.ARRAY_INDEX + i + " = 0; " +
		  RawExecutionCode.ARRAY_INDEX + i + " < " + dims[i] + " ; " +
		  RawExecutionCode.ARRAY_INDEX + i + "++)\n");
	}

	if(KjcOptions.altcodegen || KjcOptions.decoupled) {
	    print("{\n");
	    print(Util.staticNetworkSendPrefix(Util.getBaseType(tapeType)));
	    val.accept(this);
	    for (int i = 0; i < dims.length; i++) {
		print("[" + RawExecutionCode.ARRAY_INDEX + i + "]");
	    }
	    print(Util.staticNetworkSendSuffix());
	    print(";\n}\n");
	} else {
	    print("{");
	    print("static_send((" + baseType + ") ");
	    val.accept(this);
	    for (int i = 0; i < dims.length; i++) {
		print("[" + RawExecutionCode.ARRAY_INDEX + i + "]");
	    }
	    print(");\n}\n");
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
        print("register_receiver(");
        portal.accept(this);
        print(", data->context, ");
        print(self.getItable().getVarDecl().getIdent());
        print(", LATENCY_BEST_EFFORT);");
        // (But shouldn't there be a latency field in here?)
    }
    
    public void visitRegSenderStatement(SIRRegSenderStatement self,
                                        String fn,
                                        SIRLatency latency)
    {
        print("register_sender(this->context, ");
        print(fn);
        print(", ");
        latency.accept(this);
        print(");");
    }


    // ----------------------------------------------------------------------
    // UTILS
    // ----------------------------------------------------------------------

    /**
     * prints an array length expression
     */
    public void visitSwitchLabel(JSwitchLabel self,
                                 JExpression expr) {
        newLine();
        if (expr != null) {
            print("case ");
            expr.accept(this);
            print(": ");
        } else {
            print("default: ");
        }
    }

    /**
     * prints an array length expression
     */
    public void visitSwitchGroup(JSwitchGroup self,
                                 JSwitchLabel[] labels,
                                 JStatement[] stmts) {
        for (int i = 0; i < labels.length; i++) {
            labels[i].accept(this);
        }
        pos += TAB_SIZE;
        for (int i = 0; i < stmts.length; i++) {
            newLine();
            stmts[i].accept(this);
        }
        pos -= TAB_SIZE;
    }

    /**
     * prints a boolean literal
     */
    public void visitBooleanLiteral(boolean value) {
        if (value)
            print(1);
        else
            print(0);
    }

    /**
     * prints a byte literal
     */
    public void visitByteLiteral(byte value) {
        print("((byte)" + value + ")");
    }

    /**
     * prints a character literal
     */
    public void visitCharLiteral(char value) {
        switch (value) {
        case '\b':
            print("'\\b'");
            break;
        case '\r':
            print("'\\r'");
            break;
        case '\t':
            print("'\\t'");
            break;
        case '\n':
            print("'\\n'");
            break;
        case '\f':
            print("'\\f'");
            break;
        case '\\':
            print("'\\\\'");
            break;
        case '\'':
            print("'\\''");
            break;
        case '\"':
            print("'\\\"'");
            break;
        default:
            print("'" + value + "'");
        }
    }

    /**
     * prints a double literal
     */
    public void visitDoubleLiteral(double value) {
        print("((float)" + value + ")");
    }

    /**
     * prints a float literal
     */
    public void visitFloatLiteral(float value) {
        print("((float)" + value + ")");
    }

    /**
     * prints a int literal
     */
    public void visitIntLiteral(int value) {
        print(value);
    }

    /**
     * prints a long literal
     */
    public void visitLongLiteral(long value) {
        print("(" + value + "L)");
    }

    /**
     * prints a short literal
     */
    public void visitShortLiteral(short value) {
        print("((short)" + value + ")");
    }

    /**
     * prints a string literal
     */
    public void visitStringLiteral(String value) {
        print('"' + value + '"');
    }

    /**
     * prints a null literal
     */
    public void visitNullLiteral() {
        print("null");
    }

    /**
     * prints an array length expression
     */
    public void visitFormalParameters(JFormalParameter self,
                                      boolean isFinal,
                                      CType type,
                                      String ident) {
        print(type);
        if (ident.indexOf("$") == -1) {
            print(" ");
            print(ident);
        }
    }

    /**
     * prints an array length expression
     */
    public void visitArgs(JExpression[] args, int base) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i + base != 0) {
                    print(", ");
                }
                args[i].accept(this);
            }
        }
    }

    /**
     * prints an array length expression
     */
    public void visitConstructorCall(JConstructorCall self,
                                     boolean functorIsThis,
                                     JExpression[] params)
    {
        newLine();
        print(functorIsThis ? "this" : "super");
        print("(");
        visitArgs(params, 0);
        print(");");
    }

    /**
     * prints an array initializer expression
     */
    public void visitArrayInitializer(JArrayInitializer self,
                                      JExpression[] elems)
    {
        newLine();
        print("{");
        for (int i = 0; i < elems.length; i++) {
            if (i != 0) {
                print(", ");
            }
            elems[i].accept(this);
        }
        print("}");
    }

    
    // ----------------------------------------------------------------------
    // PROTECTED METHODS
    // ----------------------------------------------------------------------

    protected void newLine() {
        p.println();
    }

    // Special case for CTypes, to map some Java types to C types.
    protected void print(CType s) {
	if (s instanceof CArrayType){
            print(((CArrayType)s).getElementType());
            print("*");
        }
        else if (s.getTypeID() == TID_BOOLEAN)
            print("int");
        else if (s.toString().endsWith("Portal"))
	    // ignore the specific type of portal in the C library
	    print("portal");
	else
            print(s.toString());
    }

    protected void printLocalType(CType s) 
    {
	if (s instanceof CArrayType){
	    print(((CArrayType)s).getElementType());
	    print("*");
	}
        else if (s.getTypeID() == TID_BOOLEAN)
            print("int");
        else if (s.toString().endsWith("Portal"))
	    // ignore the specific type of portal in the C library
	    print("portal");
	else
            print(s.toString());
    }

    protected void print(Object s) {
        print(s.toString());
    }

    protected void print(String s) {
        p.setPos(pos);
        p.print(s);
    }

    protected void print(boolean s) {
        print("" + s);
    }

    protected void print(int s) {
        print("" + s);
    }

    protected void print(char s) {
        print("" + s);
    }

    protected void print(double s) {
        print("" + s);
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
