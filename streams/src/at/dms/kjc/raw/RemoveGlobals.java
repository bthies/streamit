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
import java.util.HashSet;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import java.math.BigInteger;

public class RemoveGlobals extends at.dms.util.Utils 
    implements FlatVisitor, Constants 
{
    //these are calls that cannot be inlined but the 
    //function does not need to be inlined because it
    //does not access any global vars
    public static HashSet doNotInline;
    //these are functions that we are no deleting 
    //after the inlining
    public static HashSet functionsToKeep;

    public static void doit(FlatNode top) 
    {	
	top.accept((new RemoveGlobals()), null, true);
    }
    
    public void visitNode(FlatNode node) 
    {
	if (node.isFilter()){
	    //do nothing for these types of filters
	    if(node.contents instanceof SIRIdentity ||
	       node.contents instanceof SIRFileWriter ||
	       node.contents instanceof SIRFileReader)
		return;

	    //reset the state
	    doNotInline = new HashSet();
	    functionsToKeep = new HashSet();
	    
	    if (canConvert((SIRFilter)node.contents)) {
		System.out.println("Removing Globals for " + node.contents.getName()); 
		InlineFunctionCalls.doit((SIRFilter)node.contents);
		//add the raw main function to functions we want to keep
		functionsToKeep.add(getRawMain((SIRFilter)node.contents));
		ConvertGlobalsToLocals.doit((SIRFilter)node.contents);
	    }
	    else 
		System.out.println("Cannot Remove Globals for " + node.contents.getName()); 
	}
    }

    public static JMethodDeclaration getMethod(SIRFilter filter, String ident) 
    {
	JMethodDeclaration[] methods = filter.getMethods();
	for (int i = 0; i < methods.length; i++) {
	    if (methods[i].getName().equals(ident)) 
		return methods[i];
	}

	return null;
    }
    

    public static JMethodDeclaration getRawMain(SIRFilter filter) 
    {
	
	JMethodDeclaration rawmain = getMethod(filter, RawExecutionCode.rawMain);
	if (rawmain == null)
	    Utils.fail("Could not find raw main function");
	return rawmain;
    }
    

    /**
     * Check to see if we can inline the function calls in this 
     * filter.  We simply check if there exists any mutually recursive
     * function calls.  If not return true.
     * It also decides which function calls need to be inlined and 
     * sets the Hashset in RemoveGlobals
     **/
    private boolean canConvert(SIRFilter filter) 
    {
	JMethodDeclaration[] methods = filter.getMethods();
	
	//loop thru all helper methods and see if any has a
	//call
	
	JMethodDeclaration rawMain = RemoveGlobals.getRawMain(filter);
	
	for (int i = 0; i < methods.length; i++) {
	    //this pass does causes some strange behavior with 
	    //multidimensional arrays, so just punt
	    //do not convert if there are multidimensional arrays 
	    if (MultiDimensionalArrays.exists(methods[i]))
	    	return false;
	    
	    if (methods[i].getName().startsWith("work") ||
		methods[i].getName().startsWith("init") ||
		methods[i].getName().startsWith("initWork") ||
		methods[i].getName().startsWith(RawExecutionCode.rawMain) )
		continue;
	    
	    //see if there are any function calls in the other methods
	    if (FunctionCall.exists(methods[i], filter))
		return false;
	}

	//see if all the function calls in raw main are 
	//complete statements, i.e. not part of other expressions
	//if they are not then see if this function accesses 
	//any global vars, if not we can still remove the globals
	//this function also decides which calls need to be inlined
	if (FunctionCallInExpression(rawMain, filter))
	    return false;

	//all passed return true
	return true;
    }
    private boolean FunctionCallInExpression(JMethodDeclaration method, 
					     SIRFilter filter) 
    {
	//find all the expressions that are direct children of statements
	HashSet directChildren = DirectChildrenOfStatements.get(method);
	//check to see if all function calls are the direct 
	//children of statements, not inclosed in some other expression
	return CheckFunctionCallInExpression.exists(directChildren, method,
						    filter);
    }
    
    static class MultiDimensionalArrays extends KjcEmptyVisitor 
    {
	private static boolean found;
	
	public static boolean exists(JMethodDeclaration method) 
	{
	    found = false;
	    
	    method.accept(new MultiDimensionalArrays());
	    return found;
	}

	public void visitVariableDefinition(JVariableDefinition self,
					    int modifiers,
					    CType type,
					    String ident,
					    JExpression expr) {
	    if (expr != null) {
		expr.accept(this);
	    }
	    
	    if (type.isArrayType()) {
		if (((CArrayType)type).getArrayBound() > 1)
		    found = true;
		if (((CArrayType)type).getBaseType().isArrayType())
		    found = true;		
	    }
	}
	
	//something here that checks array dimensions
	public void visitFieldExpression(JFieldAccessExpression self,
					 JExpression left,
					 String ident)
	{
	    try {
		left.accept(this);
		if (self.getField().getType().isArrayType()) {
		    if (((CArrayType)self.getField().getType()).getArrayBound() > 1)
			found = true;
		    if (((CArrayType)self.getField().getType()).getBaseType().isArrayType())
			found = true;
		}
		
	    }
	    catch (Exception e) {
		//it is easier to catch the null pointer exceptions then check for
		//them
	    }
	}
	
	public void visitLocalVariableExpression(JLocalVariableExpression self,
						 String ident) {
	    try {
		if (self.getVariable().getType().isArrayType()) {
		    if (((CArrayType)self.getVariable().getType()).getArrayBound() > 1)
			found = true;
		}
	    }
	    catch (Exception e) {
		//it is easier to catch the null pointer exceptions then check for
		//them
	    }
	}
    }
    

    static class CheckFunctionCallInExpression extends KjcEmptyVisitor 
    {
	private static HashSet children;
	private static boolean found;
	private static SIRFilter filter;

	public static boolean exists(HashSet directChildren, 
				     JMethodDeclaration method, 
				     SIRFilter f) 
	{
	    children = directChildren;
	    filter = f;
	    
	    //assume all the function calls are direct children of statements
	    found = false;
	    method.accept(new CheckFunctionCallInExpression());
	    return found;
	}
	
	public void visitMethodCallExpression(JMethodCallExpression self,
					      JExpression prefix,
					      String ident,
					      JExpression[] args) {
	    //if we find a method call not directly a child of a statement
	    //set found to true if this method accesses global vars
	    //otherwise this will be a function call that we inline
	    //I have decided to inline as much as possible
	    
	    //this should not be necessary, but just incase
	    prefix.accept(this);
	    //the args may be function calls themselves, so we must visit 
	    //the args, this was a fun bug to find!
	    for (int i = 0; i < args.length; i++) 
		args[i].accept(this);

	    if (filter.hasMethod(self.getIdent()) && !children.contains(self)) {
		if (AccessesGlobals.accessesGlobals(self.getIdent(), filter)) {
		    found = true;
		}
		else {
		    RemoveGlobals.functionsToKeep.add
			(RemoveGlobals.getMethod(filter, ident));
		    RemoveGlobals.doNotInline.add(self);
		}
		
	    } 
	}	
    }

    //returns true if a method access globals
    static class AccessesGlobals extends KjcEmptyVisitor 
    {
	private static boolean global;
	
	public static boolean accessesGlobals(String ident, SIRFilter filter) 
	{
	    JMethodDeclaration method = null;
	    global = false;

	    for (int i = 0; i < filter.getMethods().length; i++) {
		if (ident.equals(filter.getMethods()[i].getName()))
		    method = filter.getMethods()[i];
	    }
	    
	    if (method == null)
		Utils.fail("Could not find method");
	    
	    method.accept(new AccessesGlobals());
	    return global;
	}
	
	public void visitFieldExpression(JFieldAccessExpression self,
					 JExpression left,
					 String ident)
	{
	    //no need to visit left if we are here
	    //   left.accept(this);
	    global = true;
	}
    }
    

    //finds all the expressions that are direct children of statements
    static class DirectChildrenOfStatements extends KjcEmptyVisitor 
    {
	private static HashSet children;

	public static HashSet get(JMethodDeclaration method) 
	{
	    children = new HashSet();
	    method.accept(new DirectChildrenOfStatements());
	    return children;
	}
	    
	public void visitExpressionStatement(JExpressionStatement self,
				      JExpression expr) 
	{
	    children.add(expr);
	}
    }

    static class FunctionCall extends KjcEmptyVisitor 
    {
	private static boolean methodCall;
	private static JMethodDeclaration[] methods;

	public static boolean exists(JMethodDeclaration method, SIRFilter filter) 
	{
	    methodCall = false;
	    methods = filter.getMethods();
	    //System.out.println(method.getName());
	    method.accept(new FunctionCall());
	    return methodCall;
	}
	public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {
	    //System.out.println(self.getIdent());
	    //do not count this as a method call if this is a library
	    //call (i.e. sqrt()
	    for (int i = 0; i < methods.length; i++) 
		if (self.getIdent().equals(methods[i].getName()))
		    methodCall = true;
	}

	
    }


    //inline all function calls in the raw main method
    static class InlineFunctionCalls extends ReplacingVisitor
    {
	//the filter we are currently working on
	private static SIRFilter filter;
	//the methods of the filter we are currently working on
	private static JMethodDeclaration[] methods;
	
	public static void doit(SIRFilter f) 
	{
	    filter = f;
	    methods = filter.getMethods();
	    
	    RemoveGlobals.getRawMain(filter).accept(new InlineFunctionCalls());
	}

	public Object visitExpressionStatement(JExpressionStatement self,
					   JExpression expr) {

	    //inline the function call if this is a method call expression
	    //we know that all function calls that we are inlining 
	    //are just simple statements
	    if (expr instanceof JMethodCallExpression && 
		!(RemoveGlobals.doNotInline.contains(expr))) {
		JMethodCallExpression methodCall = (JMethodCallExpression)expr;
		JBlock block = new JBlock();
		JMethodDeclaration caller = null;
		
		//if the filter itself does not define the method being called
		//then this must be a c lib call, do not inline it
		if (!filter.hasMethod(methodCall.getIdent()))
		    return self;
		
		//get the method declaration for method we are calling
		for (int i = 0; i < methods.length; i++)
		    if (methods[i].getName().equals(methodCall.getIdent()))
			caller = methods[i];
		
		if (caller == null)
		    Utils.fail("Something bad happened");

		//visit the caller so that all of its function calls are inlined
		caller.accept(this);

		//now if there are args we must set the argument values
		JExpression[] args = methodCall.getArgs();		
		for (int i = 0; i < args.length; i++) {
		    //get the parameter
		    JFormalParameter param = caller.getParameters()[i];
		    //create a new local variable
		    JVariableDefinition def =
			new JVariableDefinition(null, param.getModifiers(),
						param.getType(), param.getIdent(),
						args[i]);

		    block.addStatement(new JVariableDeclarationStatement
				       (null, def, null));
		}
    
		//inline the block of the function
		JBlock body = 
		    (JBlock)ObjectDeepCloner.deepCopy
		    (caller.getBody());
		
		block.addStatement(body);
		
		return block;
	    }
	    else {
		//just 
		return self;
	    }
	}
    }

    static class ConvertGlobalsToLocals extends ReplacingVisitor
    {
	private static SIRFilter filter;
	private static HashSet localVariables;

	public static void doit(SIRFilter f) 
	{
	    filter = f;
	    localVariables = new HashSet();
	    
	    JBlock rawMainBlock = RemoveGlobals.getRawMain(filter).getBody();
	    JFieldDeclaration[] fields = filter.getFields();
	    
	    HashSet arrays = new HashSet();

	    //move all globals var defs into the raw main function
	    for (int i = 0; i < fields.length; i++) {
		JVariableDefinition def = fields[i].getVariable();
		
		//remember all the arrays so we can 
		//zero them later
		if (def.getType().isArrayType())
		    arrays.add(def);

		localVariables.add(def);
		
		rawMainBlock.addStatementFirst
		    (new JVariableDeclarationStatement
		     (null, def, null));
	    }
	    
	    //we need to initialize all array elements to zero
	    //create a statement of the form
	    //memset(var, 0, sizeof(var));
	    Iterator arrayIt = arrays.iterator();
	    while (arrayIt.hasNext()) {
		JVariableDefinition def = (JVariableDefinition)arrayIt.next();
		//use memset to zero the array, need to calculate size
		JExpression[] args = new JExpression[3];
		
		args[0] = new JLocalVariableExpression(null, def);
		
		//set up the size of arg, just the array name...
		JExpression[] sizeofArgs = new JExpression[1];
		sizeofArgs[0] = new JLocalVariableExpression(null, def);
		
		args[1] = new JIntLiteral(0);
		
		args[2] = new JMethodCallExpression(null,
						    new JThisExpression(null),
						    "sizeof",
						    sizeofArgs);
		
		JExpressionStatement zeroArray = 
		    new JExpressionStatement(null,
					     new JMethodCallExpression(null,
								       new JThisExpression(null), 
								       "memset",
								       args),
					     null);
		
		//place the statement after the variable defs
		rawMainBlock.addStatement(fields.length, zeroArray);
	    }

	    //remove the field defs from the filter
	    filter.setFields(new JFieldDeclaration[0]);

	    //convert all access of globals into locals
	    RemoveGlobals.getRawMain(filter).accept(new ConvertGlobalsToLocals());

	    //remove all the functions except the raw main and 
	    //anything we could not inline
	    filter.setMethods
		((JMethodDeclaration[])
		 RemoveGlobals.functionsToKeep.toArray(new JMethodDeclaration[0]));
	    
	    //set the init to a dummy init if we need an init 
	    if (filter.needsInit()) {
		filter.setInit(SIRStream.makeEmptyInit());
	    }

	    //set the work to a dummy work if we need a work
	    if (filter.needsWork()) {
		filter.setWork(new JMethodDeclaration(null, ACC_PUBLIC, 
						      CStdType.Void, 
						      "work",
						      new JFormalParameter[0], 
						      new CClassType[0],
						      new JBlock(),
						      null, null));
	    }
	    
	}

	private JVariableDefinition getVarDef(String ident) 
	{
	    Iterator it = localVariables.iterator();
	    
	    while(it.hasNext()) {
		JVariableDefinition def = (JVariableDefinition) it.next();
		if (def.getIdent().equals(ident))
		    return def;
	    }
	    
	    Utils.fail("Could not find var def for global variable " + 
		       ident);
	    
	    return null;
	}

	public Object visitFieldExpression(JFieldAccessExpression self,
					   JExpression left,
					   String ident)
	{
	    //if this field access is just a this expression, convert it
	    //to a local variable access
	    //otherwise it is accessing a field of a variable, so just
	    //keep the name, it does not matter what it is...
	    if (left instanceof JThisExpression)
		return new JLocalVariableExpression(null, getVarDef(ident));
	    else 
		return self;
	}
	
    }
}

