package at.dms.kjc.rstream;

import java.util.Vector;
import at.dms.kjc.common.*;
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
import java.util.HashSet;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import at.dms.util.SIRPrinter;
import at.dms.kjc.raw.*;

/**

 */

public class GenerateCCode 
{
    private Vector fields;
    private JBlock main;
    private JMethodDeclaration mainMethod;
    private Vector functions;
    private JBlock initFunctionCalls;
    private JBlock steady;
    private JBlock init;

    private FlatIRToRS toRS;

    private static String FNAME = "str.c";

    private ConvertArrayInitializers arrayInits;
    private NewArrayExprs newArrayExprs;
    
    public static void generate(FlatNode top) 
    {
	new GenerateCCode(top);
    }
    
    private GenerateCCode(FlatNode top) 
    {
	//initialize containers
	main = new JBlock(null, new JStatement[0], null);
	steady = new JBlock(null, new JStatement[0], null);
	init = new JBlock(null, new JStatement[0], null);
	fields = new Vector();
	functions = new Vector();
	initFunctionCalls = new JBlock(null, new JStatement[0], null);

	renameFilterContents(top);

	arrayInits = new ConvertArrayInitializers();
	
	visitGraph(top, true);

	visitGraph(top, false);

	setUpSIR();
	writeCompleteFile();
    }
    
    private void renameFilterContents(FlatNode top) 
    {
	top.accept(new FlatVisitor() {
		public void visitNode(FlatNode node) 
		{
		    if (node.isFilter())
			RenameAll.renameFilterContents((SIRFilter)node.contents);
		}
	    }, null, true);
    }
    

    //make everything into legal sir
    private void setUpSIR() 
    {
	//add things to the main method, the only thing
	//before this should be peek buffer declarations
	placeFieldArrayInits();
	//add the initfunction calls
	main.addStatement(initFunctionCalls);
	
	//add the init schedule 
	main.addStatement(init);
	//add the steady schedule
	main.addStatement(new JWhileStatement(null,
					      new JBooleanLiteral(null, true),
					      steady, null));

	//add the return statement
	main.addStatement(new JReturnStatement(null, 
					       new JIntLiteral(0),
					       null));
	mainMethod = 
	    new JMethodDeclaration(null, 0, CStdType.Integer,
				   MAINMETHOD,
				   new JFormalParameter[0],
				   new CClassType[0],
				   main, null, null);
    }
   

    private void writeCompleteFile() 
    {
	StringBuffer str = new StringBuffer();


	//remember all new array expression
	newArrayExprs = NewArrayExprs.doit(fields, functions, mainMethod);

	toRS = new FlatIRToRS(newArrayExprs);

	//if there are structures in the code, include
	//the structure definition header files
	if (StrToRStream.structures.length > 0) 
	    str.append("#include \"structs.h\"\n");

	str.append(getExterns());

	for(int i = 0; i < fields.size(); i++) 
	    ((JFieldDeclaration)fields.get(i)).accept(toRS);

	//initially just print the function decls
	toRS.declOnly = true;
	for (int i = 0; i < functions.size(); i++) 
	    ((JMethodDeclaration)functions.get(i)).accept(toRS);

	mainMethod.accept(toRS);

	//now print the method bodies...
	toRS.declOnly = false;
	for (int i = 0; i < functions.size(); i++) 
	    ((JMethodDeclaration)functions.get(i)).accept(toRS);


	mainMethod.accept(toRS);

	str.append(toRS.getString());
	
	System.out.println("Static doloops/doloop: " + toRS.staticDoLoops + " / " +
			   toRS.doLoops);

	System.out.println("Code for application written to str.c");
	try {
	    FileWriter fw = new FileWriter("str.c");
	    fw.write(str.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Unable to write application code.");
	}
    }

    private void visitGraph(FlatNode top, boolean isInit) 
    {
	Iterator traversal = BreadthFirstTraversal.getTraversal(top).iterator();

	while (traversal.hasNext()) {
	    FlatNode node = (FlatNode)traversal.next();
	    //	    System.out.println("Generating Code for  " + node.contents);
	    generateCode(node, isInit);

	}
    }
    
    private void generateCode(FlatNode node, boolean isInit) 
    {
	FusionState me = FusionState.getFusionState(node);
	JBlock enclosingBlock = isInit ? init : steady;
	
	//do all the things that we should do only once or during the init phase
	if (isInit) {
	    //run code optimizations and transformations
	    if (node.isFilter())
		optimizeFilter((SIRFilter)node.contents);
	    
	    me.initTasks(fields, functions, initFunctionCalls, main);
	}
	

	//add the work function that will execute the stage
	addStmtArray(enclosingBlock, me.getWork(enclosingBlock, isInit));
    }

    //for now, just print all the common math functions as
    //external functions
    protected String getExterns() 
    {
	StringBuffer buf = new StringBuffer();
	
	buf.append("#define EXTERNC \n\n");
	buf.append("extern EXTERNC int printf(char[], ...);\n");
	buf.append("extern EXTERNC int fprintf(int, char[], ...);\n");
	buf.append("extern EXTERNC int fopen(char[], char[]);\n");
	buf.append("extern EXTERNC int fscanf(int, char[], ...);\n");
	buf.append("extern EXTERNC float acosf(float);\n"); 
	buf.append("extern EXTERNC float asinf(float);\n"); 
	buf.append("extern EXTERNC float atanf(float);\n"); 
	buf.append("extern EXTERNC float atan2f(float, float);\n"); 
	buf.append("extern EXTERNC float ceilf(float);\n"); 
	buf.append("extern EXTERNC float cosf(float);\n"); 
	buf.append("extern EXTERNC float sinf(float);\n"); 
	buf.append("extern EXTERNC float coshf(float);\n"); 
	buf.append("extern EXTERNC float sinhf(float);\n"); 
	buf.append("extern EXTERNC float expf(float);\n"); 
	buf.append("extern EXTERNC float fabsf(float);\n"); 
	buf.append("extern EXTERNC float modff(float, float *);\n"); 
	buf.append("extern EXTERNC float fmodf(float, float);\n"); 
	buf.append("extern EXTERNC float frexpf(float, int *);\n"); 
	buf.append("extern EXTERNC float floorf(float);\n"); 	     
	buf.append("extern EXTERNC float logf(float);\n"); 
	buf.append("extern EXTERNC float log10f(float, int);\n"); 
	buf.append("extern EXTERNC float powf(float, float);\n"); 
	buf.append("extern EXTERNC float rintf(float);\n"); 
	buf.append("extern EXTERNC float sqrtf(float);\n"); 
	buf.append("extern EXTERNC float tanhf(float);\n"); 
	buf.append("extern EXTERNC float tanf(float);\n");
	return buf.toString();
    }


    /**
     * Returns a for loop that uses local variable <var> to count
     * <count> times with the body of the loop being <body>.  If count
     * is non-positive, just returns an empty statement.
     */
    public static JStatement makeForLoop(JStatement body,
					JLocalVariable var,
					JExpression count) {
	// make init statement - assign zero to <var>.  We need to use
	// an expression list statement to follow the convention of
	// other for loops and to get the codegen right.
	JExpression initExpr[] = {
	    new JAssignmentExpression(null,
				      new JLocalVariableExpression(null, var),
				      new JIntLiteral(0)) };
	JStatement init = new JExpressionListStatement(null, initExpr, null);
	// if count==0, just return init statement
	if (count instanceof JIntLiteral) {
	    int intCount = ((JIntLiteral)count).intValue();
	    if (intCount<=0) {
		// return empty statement
		return new JEmptyStatement(null, null);
	    }
	}
	// make conditional - test if <var> less than <count>
	JExpression cond = 
	    new JRelationalExpression(null,
				      Constants.OPE_LT,
				      new JLocalVariableExpression(null, var),
				      count);
	JExpression incrExpr = 
	    new JPostfixExpression(null, 
				   Constants.OPE_POSTINC, 
				   new JLocalVariableExpression(null, var));
	JStatement incr = 
	    new JExpressionStatement(null, incrExpr, null);

	return new JForStatement(null, init, cond, incr, body, null);
    }

    
    /**
     * Returns a do loop that uses local variable <var> to count
     * <count> times with the body of the loop being <body>.  If count
     * is non-positive, just returns an empty statement.
     */
    public static JStatement makeDoLoop(JStatement body,
					JLocalVariable var,
					JIntLiteral count) 
    {
	return 
	    new JDoLoopStatement(var,
			     new JIntLiteral(0),
			     count,
			     new JIntLiteral(1),
			     body,
			     true, //count up
			     true); //zero increment
    }



    public static JVariableDefinition newIntLocal(String prefix, int uniqueID, int initVal) 
    {
	return new JVariableDefinition(null, 0, CStdType.Integer,
				       prefix + uniqueID,  
				       new JIntLiteral(initVal));
    }

    private void optimizeFilter(SIRFilter filter) 
    {
	ArrayDestroyer arrayDest=new ArrayDestroyer();

	//iterate over all the methods, calling the magic below...
	for (int i = 0; i < filter.getMethods().length; i++) {
	    JMethodDeclaration method= filter.getMethods()[i];
	    	    
	    if (!KjcOptions.nofieldprop) {
		Unroller unroller;
		do {
		    do {
			unroller = new Unroller(new Hashtable());
			method.accept(unroller);
		    } while (unroller.hasUnrolled());
		    
		    method.accept(new Propagator(new Hashtable()));
		    unroller = new Unroller(new Hashtable());
		    method.accept(unroller);
		} while(unroller.hasUnrolled());
		
		method.accept(new BlockFlattener());
		method.accept(new Propagator(new Hashtable()));
	    } 
	    else
		method.accept(new BlockFlattener());
	    method.accept(arrayDest);
	    method.accept(new VarDeclRaiser());
	}
	
	if(KjcOptions.destroyfieldarray)
	    arrayDest.destroyFieldArrays(filter);
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
	
	//these passes have to change...

	//remove unused variables...
	RemoveUnusedVars.doit(filter);

	//remove array initializers and remember feilds for placement later...
	arrayInits.convertFilter(filter);

	//find all do loops, 
	if (StrToRStream.CONVERT_FOR_TO_DO_LOOPS)
	    IDDoLoops.doit(filter);
	//remove unnecessary do loops
	//RemoveDeadDoLoops.doit(node, toC.doloops);
    }
    

    public static void addStmtArray(JBlock block, JStatement[] stms) 
    {
	for (int i = 0; i < stms.length; i++) 
	    block.addStatement(stms[i]);
    }

    public static void addStmtArrayFirst(JBlock block, JStatement[] stms) 
    {
	int index = 0;
	for (int i = 0; i < stms.length; i++) 
	    block.addStatement(index++, stms[i]);
    }
    
    //place the array initializer blocks in the main method after
    //any array local variable declaration
    private void placeFieldArrayInits() 
    {
	Iterator blocks = arrayInits.fields.iterator();
	while (blocks.hasNext()) {
	    main.addStatement(((JBlock)blocks.next()));
	}
    }

    
    private static String MAINMETHOD = "main";
}
