package at.dms.kjc.rstream;

import java.util.Vector;
import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.*;
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

/**
 * This class is the driver that generates the c code
 * for the application.  It traverses the stream graph,
 * generating code for each filter.  It generates the init
 * fucntion calls for each filter, the initialization stage,
 * and the steady-state stage.  It writes its output to *str.c*.
 *
 *
 * @author Michael Gordon
 * 
 */

public class GenerateCCode 
{
    /* The fields of the application, convert to locals for rstream */
    private Vector fields;
    /* the main method includes the initfunction calls, init and steady states */
    private JBlock main;
    /* the main method decl */
    private JMethodDeclaration mainMethod;
    /* any helper functions of the application */
    private Vector functions;
    /** init function calls and initPath calls for joiners **/
    private JBlock initFunctionCalls;
    /** the steady state block, streams traversed in data-flow order **/
    private JBlock steady;
    /** the init state block, streams traversed in data-flow order **/
    private JBlock init;
    /** map string (variable name) to JVariableDefinition for fields, 
	set up in convertFieldsToLocals()**/
    HashMap stringVarDef = new HashMap();
    /** the c converter we are using **/
    private FlatIRToRS toRS;
    /** the name of the file we are generating **/
    private static String FNAME = "str.c";
    /** array initalizers, we must convert array inits to assignment statements**/
    private ConvertArrayInitializers arrayInits;
    /** remember the new array expressions for array decls **/
    private NewArrayExprs newArrayExprs;
    
    /**
     * The entry point of the code generation pass.  This will
     * traverse the stream graph starting at top, and generate the 
     * code for the application.
     *
     * @param top The top level node in the flat graph for 
     * the application
     *
     */
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
	//rename all filters so that names do not clash in the 
	//resulting c file
	renameFilterContents(top);
	
	arrayInits = new ConvertArrayInitializers();
	//visit the graph in for the init stage
	visitGraph(top, true);
	//visit the graph for the steady-state stage
	visitGraph(top, false);
	//set up the complete sir application
	setUpSIR();
	//write the application to the C file using FlatIRToRS
	writeCompleteFile();
    }
    
    /** rename the fields, locals, and methods of each filter
	so there are unique across filters **/
    private void renameFilterContents(FlatNode top) 
    {
	top.accept(new FlatVisitor() {
		public void visitNode(FlatNode node) 
		{
		    if (node.isFilter()) {
			RenameAll.renameFilterContents((SIRFilter)node.contents);
		    }
		    
		}
	    }, null, true);
    }
    

    /** Concatenate everything into a main method and some helper functions **/
    private void setUpSIR() 
    {
	//place any field (that are converted to locals of main) 
	//array inits into assignment statements and place them at the beginning
	//of main
	placeFieldArrayInits();
	
	//add comments to the blocks
	JavaStyleComment[] comment1 = 
	    {new JavaStyleComment("SIR: Init Schedule",
				  true, false, false)};
	init.addStatementFirst(new JEmptyStatement(null, comment1));
	JavaStyleComment[] comment2 = 
	    {new JavaStyleComment("SIR: Steady-State Schedule",
				  true, false, false)};
	steady.addStatementFirst(new JEmptyStatement(null, comment2));
	

	//add the initfunction calls
	main.addStatement(initFunctionCalls);
	
	//add the init schedule 
	main.addStatement(init);
	
	//add the steady state
	if (KjcOptions.absarray || KjcOptions.doloops) {
	    //nest inside of an rstream_pr if we are generating absarray or doloops
	    Jrstream_pr rstream_pr = new Jrstream_pr(null, steady.getStatementArray(), null);
	    JBlock whileBlock = new JBlock(null, new JStatement[0], null);
	    whileBlock.addStatement(rstream_pr);
	    main.addStatement(new JWhileStatement(null,
						  new JBooleanLiteral(null, true),
						  whileBlock,
						  null));
	}
	else {
	    //add the steady schedule
	    main.addStatement(new JWhileStatement(null,
						  new JBooleanLiteral(null, true),
						  steady, null));
	}
	//add the return statement
	main.addStatement(new JReturnStatement(null, 
					       new JIntLiteral(0),
					       null));

	//convert all fields to locals of main function
	convertFieldsToLocals();
	//construct the main driver method of the app
	mainMethod = 
	    new JMethodDeclaration(null, 0, CStdType.Integer,
				   MAINMETHOD,
				   new JFormalParameter[0],
				   new CClassType[0],
				   main, null, null);
    }
    
    /** convert all fields to locals of main function **/
    private void convertFieldsToLocals() 
    {
	//add all fields to the main method as locals
	for(int i = 0; i < fields.size(); i++) {
	    JFieldDeclaration field = 
		(JFieldDeclaration)fields.get(i);
	    main.addStatementFirst
		(new JVariableDeclarationStatement(null,
						   field.getVariable(),
						   null));
	    //remember the vardef for the visiter down below
	    //this works because we renamed everything!
	    stringVarDef.put(field.getVariable().getIdent(), field.getVariable());
	}
	
	//convert all field accesses to local accesses
	main.accept(new SLIRReplacingVisitor() {
		public Object visitFieldExpression(JFieldAccessExpression self,
						   JExpression left,
						   String ident)
		{
		    //if not a this expression, then we have a field access of a 
		    //variable that is a structure.  So just visit the expression
		    //and construct a new field access
		    if (!(left instanceof JThisExpression))
			return new JFieldAccessExpression(null, (JExpression)left.accept(this), 
							  ident,
							  null);

		    assert (stringVarDef.containsKey(ident)) &&
			(stringVarDef.get(ident) instanceof JVariableDefinition) :
			"Error converting fields to locals, name not found";
		    //return a local variable expression
		    return new JLocalVariableExpression(null, 
							((JVariableDefinition)stringVarDef.get(ident)));
		}
	    });
    }
    
   
    /** Now, the main method has been constructed, so convert the SIR 
	for the application to C code. **/
    private void writeCompleteFile() 
    {
	StringBuffer str = new StringBuffer();


	//remember all new array expressions for use in FlatIRToRS
	newArrayExprs = NewArrayExprs.doit(functions, mainMethod);
	
	toRS = new FlatIRToRS(newArrayExprs);

	//if there are structures in the code, include
	//the structure definition header files
	if (StrToRStream.structures.length > 0) 
	    str.append("#include \"structs.h\"\n");

	str.append(getExterns());

	//fields are now added as locals to main method...
	//	for(int i = 0; i < fields.size(); i++) 
	//    ((JFieldDeclaration)fields.get(i)).accept(toRS);

	//initially just print the function decls
	toRS.declOnly = true;
	for (int i = 0; i < functions.size(); i++) 
	    ((JMethodDeclaration)functions.get(i)).accept(toRS);
	//print the main method decl
	mainMethod.accept(toRS);

	//now print the method bodies...
	toRS.declOnly = false;
	for (int i = 0; i < functions.size(); i++) 
	    ((JMethodDeclaration)functions.get(i)).accept(toRS);

	//main method body
	mainMethod.accept(toRS);
	//append the string for the c code
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

    /** visit each node in the flat graph generating the SIR imperative code 
	necesary for its execution in the init (*isInit* == true) or steady
	*isInit == false*.
    **/
    private void visitGraph(FlatNode top, boolean isInit) 
    {
	//get a data-flow ordered traversal for the graph, i.e. a node 
	//can only fire if its upstream filters have fired
	Iterator traversal = DataFlowTraversal.getTraversal(top).iterator();

	while (traversal.hasNext()) {
	    FlatNode node = (FlatNode)traversal.next();
	    //System.out.println("Generating Code (" + isInit + ") for  " + node.contents);
	    generateCode(node, isInit);

	}
    }
    
    /** for each node in the graph visited by *visitGraph()*, 
	create the SIR code for its imperative execution, placing the
	code in the correct container **/
    private void generateCode(FlatNode node, boolean isInit) 
    {
	//get the fusion state for this node
	FusionState me = FusionState.getFusionState(node);
	//the block to add the code to
	JBlock enclosingBlock = isInit ? init : steady;
	
	//do all the things that we should do only once or during the init phase
	if (isInit) {
	    //run code optimizations and transformations
	    if (node.isFilter())
		optimizeFilter((SIRFilter)node.contents);
	    //tell the node to do all of its initialization
	    me.initTasks(fields, functions, initFunctionCalls, main);
	}
	

	//add the work function that will execute the stage
	addStmtArray(enclosingBlock, me.getWork(enclosingBlock, isInit));
    }

    /** for now, just print all the common math functions as
	external functions **/
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
     * Returns a for loop that uses local variable *var* to count
     * *count* times with the body of the loop being *body*.  If count
     * is non-positive, just returns an empty statement.
     */
    public static JStatement makeForLoop(JStatement body,
					JLocalVariable var,
					JExpression count) {
	// make init statement - assign zero to *var*.  We need to use
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
	// make conditional - test if *var* less than *count*
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
     * Returns a do loop that uses local variable *var* to count
     * *count* times with the body of the loop being *body*.  If count
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


    /**
     * Return a new integer local variable definition with name
     * *prefix* + *uniqueID*, that is initialized to *initVal*.
     **/
    public static JVariableDefinition newIntLocal(String prefix, int uniqueID, int initVal) 
    {
	return new JVariableDefinition(null, 0, CStdType.Integer,
				       prefix + uniqueID,  
				       new JIntLiteral(initVal));
    }

    /** call some magic for each filter to optimize it: unroller if 
	enabled, flattener, const prop, etc. **/
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
	//destroy field arrays if possible...
	if(KjcOptions.destroyfieldarray)
	    arrayDest.destroyFieldArrays(filter);

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

    /** given a block *block*, add Statement array *stms* to the
	end of the block, if stms is empty,  do nothing **/
    public static void addStmtArray(JBlock block, JStatement[] stms) 
    {
	for (int i = 0; i < stms.length; i++) 
	    block.addStatement(stms[i]);
    }
    
    /** given a block *block*, add Statement array *stms* to the
	beginning of the block, if stms is empty,  do nothing **/
    public static void addStmtArrayFirst(JBlock block, JStatement[] stms) 
    {
	int index = 0;
	for (int i = 0; i < stms.length; i++) 
	    block.addStatement(index++, stms[i]);
    }
    
    /** place the array initializer blocks in the main method after
	any array local variable declaration **/
    private void placeFieldArrayInits() 
    {
	Iterator blocks = arrayInits.fields.iterator();
	while (blocks.hasNext()) {
	    main.addStatement(((JBlock)blocks.next()));
	}
    }

    
    private static String MAINMETHOD = "main";
}
