package at.dms.kjc.rstream;

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

/**
 * This class converts the Stream IR (which references the Kopi Java IR)
 * to C code and dumps it to a file, str.c.    
 *
 *
 * @author Michael Gordon
 */
public class FlatIRToRS extends ToC 
{
    
    /** the hashmap of for loops -> do loops **/   
    private HashMap doloops;
    /** the current filter we are visiting **/
    private SIRFilter filter;
    /** comment me **/
    private NewArrayExprs newArrayExprs;
    /** > 0 if in a for loop header during visit **/
    private int forLoopHeader = 0;

    public int doLoops = 0;
    public int staticDoLoops = 0;
    

    private static final String ARRAY_COPY = "__array_copy__";
    
    public FlatIRToRS() 
    {
	this.str = new StringWriter();
        this.p = new TabbedPrintWriter(str);
	doloops = new HashMap();
	this.newArrayExprs = null;
    }
    
    
    public FlatIRToRS(NewArrayExprs newArrayExprs)
    {
	this.str = new StringWriter();
        this.p = new TabbedPrintWriter(str);
	doloops = new HashMap();
	this.newArrayExprs = newArrayExprs;
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

	//we are assigning an array to an array in C, we want to do 
	//element-wise copy!!
	
	if (!StrToRStream.GENERATE_ABSARRAY && 
	    ((left.getType() != null && left.getType().isArrayType()) ||
	    (right.getType() != null && right.getType().isArrayType()))) {
	    
	    arrayCopy(left, right);
	    return;
	}

	lastLeft=left;
        print("(");
        left.accept(this);
        print(" = ");
        right.accept(this);
        print(")");
    }

    /**
     * prints a field declaration
     */
    public void visitFieldDeclaration(JFieldDeclaration self,
                                      int modifiers,
                                      CType type,
                                      String ident,
                                      JExpression expr) {
	newLine();
	assert !(expr instanceof JNewArrayExpression) :
	    "New Array expression in field declaration";

	/*
	if (expr instanceof JArrayInitializer) {
	    declareInitializedArray(type, ident, expr);
	    return;
	}
	*/
	
	//we have an array declaration
	if (type.isArrayType()) {
	    //print the declaration and get the number of dimensions
	    int dim = handleArrayDecl(ident, type);
	    //now, get the new array expression
	    if (expr == null) { //if their isn't a new array expression in the declaration
		expr = getNewArrayExpr(ident);
	    }
	    //make sure we found a new array expression
	    if (expr instanceof JNewArrayExpression) {
		//make sure the new array expression has the correct number of dims
		assert dim == ((JNewArrayExpression)expr).getDims().length :
		    "Array " + ident + " has underspecified NewArrayExpression";
	    }
	    else {
		assert false : 
		    "Trying to initialize array with something other than NewArrayExpression";
	    }
	    
	    //print the = for the absarray();
	    if (StrToRStream.GENERATE_ABSARRAY)
		print(" = ");
	    
	    //visit the new array expression
	    expr.accept(this);
	}
	else {
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
	    
	}
	print(";");
    }

    /** return the dimensionality of this type **/
    private int getDim(CType type) 
    {
	int dim = 1;
	CType currentType = ((CArrayType)type).getElementType();
	
	while (currentType.isArrayType()) {
	    dim++;
	    currentType = ((CArrayType)currentType).getElementType();
	}    
	
	return dim;
    }
    

    /**
     * print an abstract array declaration and return the number of dimensions
     **/
    private int handleArrayDecl(String ident, CType type)
    {
	
	String brackets = StrToRStream.GENERATE_ABSARRAY ? "[[" : "";
	int dim = 1;

	CType currentType = ((CArrayType)type).getElementType();
	//keep stripping off array types until we get a base type
	while (currentType.isArrayType()) {
	    dim++;
	    brackets = brackets + 
		(StrToRStream.GENERATE_ABSARRAY ? "," : "[]");
	    currentType = ((CArrayType)currentType).getElementType();
	}
	
	if (StrToRStream.GENERATE_ABSARRAY)
	    brackets = brackets + "]]";
	else
	    brackets = brackets + "[]";
	
	//current type should now be the base type
	print(currentType);
	print(" ");
	print(ident);
	if (StrToRStream.GENERATE_ABSARRAY) 
	    print(brackets);
	return dim;
    }
    
    
    private void printArrayType(CArrayType type) 
    {
	String brackets = StrToRStream.GENERATE_ABSARRAY ? "[[" : "";
	
	CType currentType = ((CArrayType)type).getElementType();
	//keep stripping off array types until we get a base type
	while (currentType.isArrayType()) {
	    brackets = brackets + 
		(StrToRStream.GENERATE_ABSARRAY ? "," : "*");
	    currentType = ((CArrayType)currentType).getElementType();
	}
	
	if (StrToRStream.GENERATE_ABSARRAY)
	    brackets = brackets + "]]";
	else 
	    brackets = brackets + "*";
	
	//current type should now be the base type
	print(currentType);
	print(" ");
	print(brackets);
    }
    

    /** 
     * given a string (for field) or a JVariableDefinition (for locals)
     * find the corresponding JNewArrayExpression, return null if none was found
    **/
    private JNewArrayExpression getNewArrayExpr(Object var) 
    {
	JNewArrayExpression expr = null; 

	if (newArrayExprs.getNewArr(var) != null) 
	    expr = newArrayExprs.getNewArr(var);
	else {  //otherwise, this array was assinged another array,
	    Object current = var; //so look for that array's new array expression
	    while (expr == null) {  //keep going until we find the new array expression
		if (newArrayExprs.getNewArr(newArrayExprs.getArrAss(current)) != null)
		    expr = newArrayExprs.getNewArr(newArrayExprs.getArrAss(current));
		else 
		    current = newArrayExprs.getArrAss(current);
	    }
	}
	return expr;
    }
    

    /**
     * prints a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {
	
	/*if (expr instanceof JArrayInitializer) {
		declareInitializedArray(type, ident, expr);
		return;
		}*/
	
	//we have an array declaration
	if (type.isArrayType()) {
	    //print the declaration and get the number of dimensions
	    int dim = handleArrayDecl(ident, type);
	    //now, get the new array expression
	    if (expr == null) {//if their isn't a new array expression in the declaration
		expr = getNewArrayExpr(self);
	    }
	    //make sure we found a new array expression
	    if (expr instanceof JNewArrayExpression) {
		//make sure the new array expression has the correct number of dims
		assert dim == ((JNewArrayExpression)expr).getDims().length :
		    "Array " + ident + " has underspecified NewArrayExpression";
	    }
	    else {
		assert false : 
		    "Trying to initialize array with something other than a new array expression";
	    }
	    
	    
	    if (StrToRStream.GENERATE_ABSARRAY) {
		print(" = ");
	    }
	    
	    if (expr != null)
		expr.accept(this);

	}
	else {
	    print(type);
	    
	    print(" ");
	    print(ident);
	    if (expr != null) {
		print(" = ");
		expr.accept(this);
	    } else {
		if (type.isOrdinal())
		    print (" = 0");
		else if (type.isFloatingPoint())
		    print(" = 0.0f");
	    }
	    
	}
	print(";");
    }

    /**
     * prints an array allocator expression
     */
    public void visitNewArrayExpression(JNewArrayExpression self,
                                        CType type,
                                        JExpression[] dims,
                                        JArrayInitializer init)
    {
	//we should see no zero dimension arrays
	assert dims.length > 0 : "Zero Dimension array" ;
	//and no initializer
	assert init == null : "Initializers of Abstract Arrays not supported in RStream yet";
	if (StrToRStream.GENERATE_ABSARRAY) {
	    //we are generating abstract arrays
	    //print the absarray call with the dimensions...
	    print(" absarray" + dims.length + "(");
	    dims[0].accept(this);
	    for (int i = 1; i < dims.length; i++) {
		print(",");
		dims[i].accept(this);
	    }
	    print(")");
	}
	else {
	    //normal c arrays
	    for (int i = 0; i < dims.length; i++) {
		print("[");
		dims[i].accept(this);
		print("]");
	    }
	}
    }
    
    private int[] getDims(JNewArrayExpression newArray) 
    {
	int dims[] = new int[newArray.getDims().length];
	
	for (int i = 0; i < dims.length; i++) {
	    assert newArray.getDims()[i] instanceof JIntLiteral;
	    dims[i] = ((JIntLiteral)newArray.getDims()[i]).intValue();
	}
	return dims;
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
        newLine();
	// print(CModifier.toString(modifiers));
	print(returnType);
	print(" ");
	
	//just print initPath() instead of initPath<Type>
	//if (ident.startsWith("initPath"))
	//    print("initPath"); 
	//else
	
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

	//set the current method we are visiting
	method = self;
	
	print(" ");
        if (body != null) 
	    body.accept(this);
        else 
            print(";");

        newLine();
	method = null;
    }
    
    
    

    // ----------------------------------------------------------------------
    // STATEMENT
    // ----------------------------------------------------------------------   

    public void visitDoLoopStatement(JDoLoopStatement self) 
    {
	assert self.countUp() : "Currently we only handle doloops with positive increment";

	doLoops++;
	if (self.staticBounds())
	    staticDoLoops++;

	print("doloop (");
	print(self.getInduction().getType() + " ");
	print(self.getInduction().getIdent());
	print(" = ");
	self.getInitValue().accept(this);
	print("; ");
	self.getCondValue().accept(this);
	print("; ");
	self.getIncrValue().accept(this);
	print(") ");

	
	
	newLine();
        pos += TAB_SIZE;
        self.getBody().accept(this);
        pos -= TAB_SIZE;
        newLine();
    }
    

    /**
     * prints a for statement
     */
    public void visitForStatement(JForStatement self,
                                  JStatement init,
                                  JExpression cond,
                                  JStatement incr,
                                  JStatement body) {

	if (StrToRStream.GENERATE_DO_LOOPS && self instanceof JDoLoopStatement) {
	    visitDoLoopStatement((JDoLoopStatement)self);
	    return;
	}
	
	//be careful, if you return prematurely, decrement me
	forLoopHeader++;

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
	    FlatIRToRS l2c = new FlatIRToRS(newArrayExprs);
	    l2c.doloops = this.doloops;
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
	forLoopHeader--;
	print(") ");
	
        //print("{");
	newLine();
        pos += TAB_SIZE;
        body.accept(this);
        pos -= TAB_SIZE;
        newLine();
	//print("}");
    }



    /**
     * prints an array access expression
     */
    public void visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor) {
	if (StrToRStream.GENERATE_ABSARRAY) {
	    String access = "[[";
	    JExpression exp = prefix;
	    
	    //if this is a multidimensional access, convert to the 
	    //comma'ed form
	    while (exp instanceof JArrayAccessExpression) {
		JArrayAccessExpression arr = (JArrayAccessExpression)exp;
		FlatIRToRS toRS = new FlatIRToRS(newArrayExprs);
		arr.getAccessor().accept(toRS);
		
		access = access + toRS.getString() + ", ";
		exp = arr.getPrefix();
	    }
	    //visit the var access
	    exp.accept(this);
	    print(access);
	    accessor.accept(this);
	    print("]]");
	}
	
	else {
	    //normal c arrays
	    String access = "";
	    JExpression exp = prefix;
	    while (exp instanceof JArrayAccessExpression) {
		JArrayAccessExpression arr = (JArrayAccessExpression)exp;
		FlatIRToRS toRS = new FlatIRToRS(newArrayExprs);
		arr.getAccessor().accept(toRS);
		
		access = access + "[" + toRS.getString() + "]";
		exp = arr.getPrefix();
	    }
	    exp.accept(this);
	    print(access);
	    print("[");
	    accessor.accept(this);
	    print("]");
	}
    }
    

    /**
     * prints a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {

	assert (!ident.equals(Names.receiveMethod)) :
	    "Error: RStream code generation should not see network receive method";
	
        print(ident);
	
	//we want single precision versions of the math functions
	if (Utils.isMathMethod(prefix, ident)) 
	    print("f");
	    
	print("(");
	if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i != 0) {
                    print(", ");
                }
		/* this is a hack but there is no other way to do it,
		   if we are currently visiting fscanf and we are at the 3rd
		   argument, prepend an & to get the address and pass the pointer 
		   to the fscanf
		*/
		if (ident.equals(Names.fscanf) && i == 2)
		    print("&");
                args[i].accept(this);
            }
        }
        print(")");
    }

    public JExpression passParentheses(JExpression exp) 
    {
	while (exp instanceof JParenthesedExpression)
	    exp = ((JParenthesedExpression)exp).getExpr();
	
	return exp;
    }
    

    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
                                    JExpression num)
    {
	assert false : "RStream code generation should not see a pop statement";
    }
    
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType)
    {
	assert false : "RStream code generation should not see a pop statement";
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
		print("printf(\"%d\\n\", "); 
		exp.accept(this);
		print(");");
	    }
	else if (type.equals(CStdType.Char))
	    {
		print("printf(\"%d\\n\", "); 
		exp.accept(this);
		print(");");
	    }
	else if (type.equals(CStdType.Float))
	    {
		print("printf(\"%f\\n\", ");
		exp.accept(this);
		print(");");
	    }
        else if (type.equals(CStdType.Long))
	    {
		print("printf(\"%d\\n\", "); 
		exp.accept(this);
		print(");");
	    }
	else if (type.equals(CStdType.String)) 
	    {
		print("printf(\"%s\\n\", "); 
		exp.accept(this);
		print(");");
	    }
	else
	    {
		assert false : "Unprintable type";
	    }
    }

    
    public void visitPushExpression(SIRPushExpression self,
                                    CType tapeType,
                                    JExpression val)
    {
	assert false : "RStream Front-end should not see a push statement";
    }    

    
    

    // Special case for CTypes, to map some Java types to C types.
    protected void print(CType s) {
	if (s instanceof CArrayType){
	    printArrayType((CArrayType)s);
	    //assert false : "Should not be printing an array type";
        }
        else if (s.getTypeID() == TID_BOOLEAN)
            print("int");
        else if (s.toString().endsWith("Portal"))
	    // ignore the specific type of portal in the C library
	    print("portal");
	else
            print(s.toString());
    }
    

    /** This function is called if we have an assignment expression of array types 
	and we are generating C code.  This will generate code to perform an 
	element-wise copy **/
    private void arrayCopy(JExpression left, 
			   JExpression right) 
    {
	String ident = "";
	//this is used to find the new array expression
	//it is either a string for fields or JVarDef for locals
	Object varDef = null;
	//the var access expression
	JExpression var = left;
	
	//if this is an array access expression, get the variable access
	if (left instanceof JArrayAccessExpression) {
	    var = Util.getVar((JArrayAccessExpression)left);
	}
	

	if (var instanceof JFieldAccessExpression) {
	    varDef = ((JFieldAccessExpression)var).getIdent();
	    ident = ((JFieldAccessExpression)var).getIdent();
	}
	else if (var instanceof JLocalVariableExpression) {
	    varDef = ((JLocalVariableExpression)var).getVariable();
	    ident = ((JLocalVariableExpression)var).getVariable().getIdent();
	}
	else 
	    Utils.fail("Assigning an array to an unsupported expression of type " + left.getClass() + ": " + left);
	
	//	assert getDim(left.getType()) == getDim(right.getType()) :
	//    "Array dimensions of variables of array assignment do not match";
	
	//find the number of dimensions
	int bound = getDim(right.getType());
	//find the extent of each dimension
	int[] dims = getDims(getNewArrayExpr(varDef));
	//if we are assigning elements from a lower dimension array to a higher
	//dim array, remember the difference
	int diff = dims.length - bound;
	
	assert diff >= 0 : "Error in array copy: " + left + " = " + right;

	assert bound > 0;

	//print out a loop that will perform the element-wise copy
	print("{\n");
	print("int ");
	//print the index var decls
	for (int i = 0; i < bound -1; i++)
	    print(FlatIRToRS.ARRAY_COPY + i + ", ");
	print(FlatIRToRS.ARRAY_COPY + (bound - 1));
	print(";\n");
	for (int i = 0; i < bound; i++) {
	    print("for (" + FlatIRToRS.ARRAY_COPY + i + " = 0; " + FlatIRToRS.ARRAY_COPY + i +  
		  " < " + dims[i + diff] + "; " + FlatIRToRS.ARRAY_COPY + i + "++)\n");
	}
	left.accept(this);
	for (int i = 0; i < bound; i++)
	    print("[" + FlatIRToRS.ARRAY_COPY + i + "]");
	print(" = ");
	right.accept(this);
	for (int i = 0; i < bound; i++)
	    print("[" + FlatIRToRS.ARRAY_COPY + i + "]");
	print(";\n}\n");
	return;
    }
    


    /**
     * prints a cast expression
     */
    public void visitCastExpression(JCastExpression self,
				    JExpression expr,
				    CType type)
    {
	//hack, if we not generating abstract arrays
	//then don't print array casts for C
	if (!StrToRStream.GENERATE_ABSARRAY && type.isArrayType()) {
	    expr.accept(this);
	    return;
	}
	
	    
	print("(");
        print("(");
	print(type);
        print(")");
        print("(");
	expr.accept(this);
	print(")");
        print(")");
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

     public void visitPhasedFilter(SIRPhasedFilter self,
                                  SIRPhasedFilterIter iter) {
        // This is a stub; it'll get filled in once we figure out how phased
        // filters should actually work.
    }

    protected void stackAllocateArray(String str) 
    {
	assert false : "Should not be called";
    }

    /**
     * prints a empty statement
     */
    public void visitEmptyStatement(JEmptyStatement self) {
	//if we are inside a for loop header, we need to print 
	//the ; of an empty statement
	if (forLoopHeader > 0) {
	    newLine();
	    print(";");
	}
    }

//     /**
//      * The main entry point of the visiting done by this class. 
//      * print out c includes, visit the methods, and then generate
//      * the main function in the c code that calls the driver function
//      * that controls execution.
//      *
//      * @param self The filter we are visiting
//      *
//      */

//     public void visitFilter(SIRFilter self,
// 			    SIRFilterIter iter) {
// 	assert false : "Don't call me!";
	
// 	//Entry point of the visitor

// 	//print("#include <stdlib.h>\n");
// 	//print("#include <math.h>\n\n");

// 	//if there are structures in the code, include
// 	//the structure definition header files
// 	if (StrToRStream.structures.length > 0) 
// 	    print("#include \"structs.h\"\n");
	
// 	printExterns();
// 	//Visit fields declared in the filter class
// 	JFieldDeclaration[] fields = self.getFields();
// 	for (int i = 0; i < fields.length; i++)
// 	   fields[i].accept(this);
	
// 	//visit methods of filter, print the declaration first
// 	declOnly = true;
// 	JMethodDeclaration[] methods = self.getMethods();
// 	for (int i =0; i < methods.length; i++)
// 	    methods[i].accept(this);
	
// 	//now print the functions with body
// 	declOnly = false;
// 	for (int i =0; i < methods.length; i++) {
// 	    methods[i].accept(this);	
// 	}
	
// 	print("int main() {\n");
// 	//generate array initializer blocks for fields...
// 	printFieldArrayInits();
	
// 	//execute the main function
// 	print(Names.main + "();\n");
	
// 	//return 0 even though this should never return!
// 	print("  return 0;\n");
// 	//closes main()
// 	print("}\n");
       
// 	createFile();
//     }

//     /**
//      * The entry method to this C conversion pass.  Given a flatnode containing
//      * the single fused filter of the application, optimize the SIR code, if
//      * enabled, and then generate then convert to C code and dump to a file.
//      *
//      * @param node The flatnode containing the single filter of the application.
//      *
//      */
//     public static void generateCode(FlatNode node) 
//     {
// 	assert false : "don't call me";
	
// 	//FlatIRToRS toC = new FlatIRToRS((SIRFilter)node.contents);
		
// 	//optimizations...
// 	System.out.println
// 	    ("Optimizing SIR ...");

// 	ArrayDestroyer arrayDest=new ArrayDestroyer();

// 	//iterate over all the methods, calling the magic below...
// 	for (int i = 0; i < ((SIRFilter)node.contents).getMethods().length; i++) {
// 	    JMethodDeclaration method=((SIRFilter)node.contents).getMethods()[i];
	    	    
// 	    if (!KjcOptions.nofieldprop) {
// 		Unroller unroller;
// 		do {
// 		    do {
// 			unroller = new Unroller(new Hashtable());
// 			method.accept(unroller);
// 		    } while (unroller.hasUnrolled());
		    
// 		    method.accept(new Propagator(new Hashtable()));
// 		    unroller = new Unroller(new Hashtable());
// 		    method.accept(unroller);
// 		} while(unroller.hasUnrolled());
		
// 		method.accept(new BlockFlattener());
// 		method.accept(new Propagator(new Hashtable()));
// 	    } 
// 	    else
// 		method.accept(new BlockFlattener());
// 	    method.accept(arrayDest);
// 	    method.accept(new VarDeclRaiser());
// 	}
	
// 	if(KjcOptions.destroyfieldarray)
// 	   arrayDest.destroyFieldArrays((SIRFilter)node.contents);
// 	   /*	
// 	     try {
// 	     SIRPrinter printer1 = new SIRPrinter();
// 	     IterFactory.createFactory().createIter((SIRFilter)node.contents).accept(printer1);
// 	     printer1.close();
// 	     }
// 	     catch (Exception e) 
// 	     {
// 	     }
// 	*/
// 	//remove unused variables...
// 	RemoveUnusedVars.doit(node);
// 	//remove array initializers and remember them for placement later...
// 	toC.arrayInits = new ConvertArrayInitializers(node);
// 	//find all do loops, 
// 	toC.doloops = IDDoLoops.doit(node);
// 	//remove unnecessary do loops
// 	//RemoveDeadDoLoops.doit(node, toC.doloops);
// 	//now iterate over all the methods and generate the c code.
//         IterFactory.createFactory().createIter((SIRFilter)node.contents).accept(toC);
//     }


    
//     private void createFile() {
// 	System.out.println("Code for application written to str.c");
// 	try {
// 	    FileWriter fw = new FileWriter("str.c");
// 	    fw.write(str.toString());
// 	    fw.close();
// 	}
// 	catch (Exception e) {
// 	    System.err.println("Unable to write tile code file for filter " +
// 			       filter.getName());
// 	}
//     }

//     //for now, just print all the common math functions as
//     //external functions
//     protected void printExterns() 
//     {
// 	print("#define EXTERNC \n\n");
// 	print("extern EXTERNC int printf(char[], ...);\n");
// 	print("extern EXTERNC int fprintf(int, char[], ...);\n");
// 	print("extern EXTERNC int fopen(char[], char[]);\n");
// 	print("extern EXTERNC int fscanf(int, char[], ...);\n");
// 	print("extern EXTERNC float acosf(float);\n"); 
// 	print("extern EXTERNC float asinf(float);\n"); 
// 	print("extern EXTERNC float atanf(float);\n"); 
// 	print("extern EXTERNC float atan2f(float, float);\n"); 
// 	print("extern EXTERNC float ceilf(float);\n"); 
// 	print("extern EXTERNC float cosf(float);\n"); 
// 	print("extern EXTERNC float sinf(float);\n"); 
// 	print("extern EXTERNC float coshf(float);\n"); 
// 	print("extern EXTERNC float sinhf(float);\n"); 
// 	print("extern EXTERNC float expf(float);\n"); 
// 	print("extern EXTERNC float fabsf(float);\n"); 
// 	print("extern EXTERNC float modff(float, float *);\n"); 
// 	print("extern EXTERNC float fmodf(float, float);\n"); 
// 	print("extern EXTERNC float frexpf(float, int *);\n"); 
// 	print("extern EXTERNC float floorf(float);\n"); 	     
// 	print("extern EXTERNC float logf(float);\n"); 
// 	print("extern EXTERNC float log10f(float, int);\n"); 
// 	print("extern EXTERNC float powf(float, float);\n"); 
// 	print("extern EXTERNC float rintf(float);\n"); 
// 	print("extern EXTERNC float sqrtf(float);\n"); 
// 	print("extern EXTERNC float tanhf(float);\n"); 
// 	print("extern EXTERNC float tanf(float);\n");
	     
//     }

 }
