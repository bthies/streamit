//===========================================================================
//
//   FILE: kopi2SIR.java:
//   
//   Author: Michael Gordon
//   Date: Wed Sep 26 23:00:44 2001
//
//   Function:  Implements the KJC to SIR pass using a KJCVisitor
//
//===========================================================================


package at.dms.kjc;

import java.io.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;
import at.dms.util.*;
import java.util.*;


public class Kopi2SIR extends Utils implements AttributeVisitor
{
    /* The entire application */
    private JCompilationUnit[] application;

    /* The  one to one parent*/
    private SIRStream parentStream;
    /* The Top Level SIR Operator */
    private SIRStream topLevel;
    
    /*Object used to disregard the return values from the attributed
      visitor methods */
    private Object trash;

    private int num = 0;
    /* the current source line number (set as part of blockstart) */
    private int lineNumber = -1;
    
    //Global state holding the current method we are processing if any 
    //set/reset in visitMethodCall
    private String currentMethod;
    
    //This hashtable acts as a symbol table to store named (not anonymous)
    //SIR classes
    private Hashtable visitedSIROps;
    //This hashtable acts as a symbol table for local variables and 
    //field variables, it is indexed by the JLocalVariable or JFieldVariable
    //and store the corresponding SIR object
    private Hashtable symbolTable;

    //This vector store all the interface declarations for a toplevel prgm
    private Vector interfaceList;

    //This vector stores all the interface tables as derived from the
    //regReceiver statements.  An interface table lists the methods
    //that implement a given interface for a given class.
    private Vector interfaceTableList;

    //globals to store the split and join type so they can be set in the
    //post visit of the class declaration
    private SIRSplitType splitType;
    private SIRJoinType joinType;

    //The current dependency chain we are following when 
    //trying to resolve a class instantiation to a stream
    //stores strings of the stream names
    private LinkedList searchList;

    //Uncomment the println for debugging
    private void printMe(String str) {
	//System.out.println(str);
    }

    public Kopi2SIR() {
	parentStream = null;
	topLevel = null;

	currentMethod = null;
	visitedSIROps = new Hashtable(100);
	symbolTable = new Hashtable(300);
	interfaceList = new Vector(100);
	interfaceTableList = new Vector(100);
	searchList = new LinkedList();
	application = null;
    }

    public Kopi2SIR(JCompilationUnit[] app) {
	parentStream = null;
	topLevel = null;

	currentMethod = null;
	visitedSIROps = new Hashtable(100);
	symbolTable = new Hashtable(300);
	interfaceList = new Vector(100);
	interfaceTableList = new Vector(100);
	searchList = new LinkedList();
	this.application = app;
    }

    private String printLine(JPhylum l) {
	return (String.valueOf(l.getTokenReference().getLine()) + ": ");
    }

    private void addVisitedOp(String className, SIROperator sirop)  {
	if (visitedSIROps.get(className) != null)
	    at.dms.util.Utils.fail("Duplicate Definition of " + className);
	visitedSIROps.put(className, sirop);
    }
    private SIROperator getVisitedOp(String className) 
    {
	//return the stream if it is in the table
	if (visitedSIROps.get(className) != null) {
	    return (SIROperator)visitedSIROps.get(className);
	}
	//if not look for it over the entire application (if given)
	else if (application != null) {
	    //if this class name is already trying to be resolved then
	    //we have a mutually recursive defintion
	    if (searchList.contains(className)) 
		at.dms.util.Utils.fail("Mutually recursive stream defintion of " + 
				       className);
	    //add this class name to the list of streams we are resolving
	    searchList.add(className);
	    for (int unit = 0; unit < application.length; unit++) {
		JTypeDeclaration[] decls = application[unit].getTypeDeclarations();
		for (int i = 0; i < decls.length; i++) {
		    if (decls[i] instanceof JClassDeclaration) {
			//if this class declaration is a match, visit it and 
			//get the SIRStream
			if (((JClassDeclaration)decls[i]).
			    getSourceClass().getIdent().equals(className)) {
			    //visit the class declaration and return the stream
			    SIROperator sir = (SIROperator)decls[i].accept(this);
			    //visitClassDecl will add the stream to the table
			    
			    //remove the name from the resolve list
			    searchList.remove(className);
			    //return the stream
			    return sir;
			}
		    }
		}
	    }
	}
	
	at.dms.util.Utils.fail(lineNumber + ": Cannot find declaration of stream " +
			       className);
	return null;
    }
    
    private void blockStart(String str)  {
	printMe(str);
    }

    private void blockStart(String str, JPhylum getLineFromMe)  {
	lineNumber = getLineFromMe.getTokenReference().getLine();
	printMe("attribute_visit" + str +"\n");
    }
    
    /* creates a new SIROperator setting parentStream to this object
       also, if the SIROperator is one to one, parentStream is set */
    private SIROperator newSIROP(JClassDeclaration clazz)  {
	String TYPE = clazz.getSourceClass().getSuperClass().getIdent();
    
	if (TYPE.equals("Pipeline") || TYPE.equals("StreamIt")) {
	    SIRPipeline current = new SIRPipeline((SIRContainer)parentStream, 
				   JFieldDeclaration.EMPTY,
				   JMethodDeclaration.EMPTY);
	    parentStream = current;
	    return current;
	}
	if (TYPE.equals("Filter")) {
	    SIRFilter current = new SIRFilter();
	    current.setParent((SIRContainer)parentStream);
	    current.setFields(JFieldDeclaration.EMPTY);
	    current.setMethods(JMethodDeclaration.EMPTY);
	    parentStream = current;
	    return current;
	}
	if (TYPE.equals("FeedbackLoop")) {
	    SIRFeedbackLoop current = new SIRFeedbackLoop();
	    current.setParent((SIRContainer)parentStream);
	    current.setFields(JFieldDeclaration.EMPTY);
	    current.setMethods(JMethodDeclaration.EMPTY);
	    parentStream = current;
	    return current;
	}
	if (TYPE.equals("SplitJoin")) {
	    SIRSplitJoin current = new SIRSplitJoin((SIRContainer)parentStream,
						     JFieldDeclaration.EMPTY,
						     JMethodDeclaration.EMPTY);
	    parentStream = current;
	    return current;
	}
						    
	at.dms.util.Utils.fail(printLine(clazz) + 
			       "You are creating an unsupported streaMIT Operator: "
			       + TYPE + ".");
	return null;
    }
    
    /**
     * Returns true if the given JStatement defines a call to 
     * an SIR expression
     **/
    private boolean isSIRExp(JMethodCallExpression exp)  
    {

	if (exp.getIdent().startsWith("push") ||
	    exp.getIdent().startsWith("pop")  ||
	    exp.getIdent().startsWith("peek") ||
	    exp.getIdent().startsWith("println") )
	    return true;
	
	return false;
    }

    /* Given a method call expression, this function will create/return a 
       SIR Expression if the method call orignally denoted one. */
    private JPhylum newSIRExp(JMethodCallExpression exp, JExpression[] args)  
    {
	JPhylum newExp = null;
	printMe("In newSIRExp");
	if (args.length > 1)
	    at.dms.util.Utils.fail(printLine(exp) +
				   "Too many args to SIR call.");
	
	if (args.length == 0)
	    args = JExpression.EMPTY;
	else
	    args[0] = (JExpression) args[0].accept(this);
	
	if (exp.getIdent().startsWith("push")) {
	    newExp = new SIRPushExpression(args[0]);
	    ((SIRPushExpression)newExp).setTapeType(((SIRFilter)parentStream).getOutputType());
	}
	if (exp.getIdent().startsWith("pop")) {
	    newExp = new SIRPopExpression();
	    ((SIRPopExpression)newExp).setTapeType(((SIRFilter)parentStream).getInputType());
	}
	if (exp.getIdent().startsWith("peek")) {
	    newExp = new SIRPeekExpression(args[0]);
	    ((SIRPeekExpression)newExp).setTapeType(((SIRFilter)parentStream).getInputType()); 
	}
	if (exp.getIdent().startsWith("println")) {
	    newExp = new SIRPrintStatement(null, args[0], null);
	}
	
	return newExp;
    }
   
    //Perform any post visiting operations after a class is declared
    private void postVisit(SIROperator current) 
    {
	if (current instanceof SIRFilter) {
	    //Check for init statement
	    if (!((SIRFilter)current).hasMethod("init"))
		 at.dms.util.Utils.fail("Filter must have an init statement.");
	}
	if (current instanceof SIRFeedbackLoop) {
	    if (splitType != null)
		((SIRFeedbackLoop)current).setSplitter(SIRSplitter.create((SIRFeedbackLoop)
									  current, splitType, 2));
	    if (joinType != null)
		((SIRFeedbackLoop)current).setJoiner(SIRJoiner.create((SIRFeedbackLoop)current, 
								      joinType, 2));
	    splitType = null;
	    joinType = null;
	}
	if (current instanceof SIRSplitJoin) {
	    if (splitType != null)
		((SIRSplitJoin)current).setSplitter(SIRSplitter.create((SIRSplitJoin)
									  current, splitType, 2));
	    if (joinType != null)
		((SIRSplitJoin)current).setJoiner(SIRJoiner.create((SIRSplitJoin)current, 
								      joinType, 2));
	    splitType = null;
	    joinType = null;
	}
       
    }

    private boolean supportedType(String type) 
    {
	if (type.equals("Integer") ||
	    type.equals("Character") ||
	    type.equals("Boolean") ||
	    type.equals("Byte") ||
	    type.equals("Double") ||
	    type.equals("Float")||
	    type.equals("Short") ||
	    type.equals("String") ||
	    type.equals("Long")) 
	    return true;
	else 
	    return false;
    }
	  

    private JMethodDeclaration[] buildPortalMethodArray(JClassDeclaration portal, 
							JClassDeclaration clazz) 
    {
	JMethodDeclaration[] portalMethods = portal.getMethods();
	JMethodDeclaration[] clazzMethods = clazz.getMethods();
	JMethodDeclaration[] handlerMethods = new JMethodDeclaration[portalMethods.length];
	
	//For each method declared in the portal class
	//find the corresponding method in the given clazz
	//build the handlerMethods as we go
	for (int i = 0; i < portalMethods.length; i++) {
	    JMethodDeclaration handlerMethod = null;
	    for (int j = 0; j < clazzMethods.length; j++) {
		//Check if the signature of the current method equals the portal method
		//this may not work, I will try it!
		if (portalMethods[i].getMethod().equals(clazzMethods[j].getMethod())) 
		    handlerMethod = clazzMethods[j];
	    }
	    if (handlerMethod == null)
		at.dms.util.Utils.fail(printLine(portal) +
				       "Cannot find a method declaration for portal method " + 
				       portalMethods[i].getName() + " in class " + 
				       clazz.getSourceClass().getIdent());
	    handlerMethods[i] = handlerMethod;
	}
	return handlerMethods;
    }
    
		    
		    
	    
	    
    public Object visitClassDeclaration(JClassDeclaration self,
                                      int modifiers,
                                      String ident,
                                      String superName,
                                      CClassType[] interfaces,
                                      JPhylum[] body,
					     JFieldDeclaration[] fields,
                                      JMethodDeclaration[] methods,
                                      JTypeDeclaration[] decls) 
    {
	/* The current SIR Operator we are creating */
	SIROperator current;
	SIRStream oldParentStream = parentStream;
	/* true if this class was created with an anonymous creation */
	boolean anonCreation = false;
	
	blockStart("ClassDeclaration", self);
	printMe("Class Name: " + ident);
	printMe(self.getSourceClass().getSuperClass().getIdent());
	
	if (ident.endsWith("Portal"))
	return null;
	
	// if the name of the class being declared is the same
	// as the name of the superclass then it is anonymous
	if (self.getSourceClass().getSuperClass().getIdent().equals(ident))
	    anonCreation = true;

	// create a new SIROperator
	current = newSIROP(self);

	//If this class is public then it is the topLevel stream (entry point)
	if (self.getSourceClass().getSuperClass().getIdent().equals("StreamIt")) {
	    if (topLevel == null)
		topLevel = (SIRStream) current;
	    else
		at.dms.util.Utils.fail(printLine(self) +
				       "Toplevel Stream already set");	
	}
	if (current == null) 
	    printMe("Null");
	
	trash = visitClassBody(decls, fields, methods, body);

	/* add the current SIR OP to the parent */
	
	if (current == null) 
	    printMe("Null");
	
	/* Perform any operations needed after the childern are visited */
	postVisit(current);

	/*
	  if this is not an anonymous creation add the SIR op to the 
	   "symbol table" 
	*/
	if (!anonCreation)
	    addVisitedOp(ident, current);
	     
	parentStream = oldParentStream;
	printMe( "Out " + self.getSourceClass().getSuperClass().getIdent()
			    + num);
	return current;
    }

    public Object visitCompilationUnit(JCompilationUnit self,
                                     JPackageName packageName,
                                     JPackageImport[] importedPackages,
                                     JClassImport[] importedClasses,
                                     JTypeDeclaration[] typeDeclarations)
    {
	/* The beginning of the visit! */
	
	//install a try to catch all runtime errors and report the line
	//number
	try {
	    blockStart("CompilationUnit", self);
	    boolean isTopLevel = false;
	    
	    /*if (packageName.getName().length() > 0)
	      packageName.accept(this);*/
	    
	    /*for (int i = 0; i < importedPackages.length ; i++)
	      importedPackages[i].accept(this);*/
	    
	    /*  for (int i = 0; i < importedClasses.length ; i++)
		importedClasses[i].accept(this);*/


	    //check to see if this is the top level compilation unit
	    //by checking all of the class declarations
	    for (int i = 0; i < typeDeclarations.length ; i++) {
		if (typeDeclarations[i] instanceof JClassDeclaration) {
		    JClassDeclaration decl = (JClassDeclaration)
			typeDeclarations[i];
		    if (decl.getSourceClass().getSuperClass().
			getIdent().equals("StreamIt")) {
			if (isTopLevel)
			    at.dms.util.Utils.fail(printLine(decl) + 
						   "Top level stream already twice.");
			isTopLevel = true;
		    }
		}
	    }
	    
	    //visit the file only if it is the top level!!!!
	    if (isTopLevel)	    
		for (int i = 0; i < typeDeclarations.length ; i++)
		    trash = typeDeclarations[i].accept(this);
	    
	    
	    
	    /* the end of the visit */
	}
	catch (Throwable t) {
	    System.err.println("Error at or around line " + lineNumber);
	    System.err.println("Stack Trace:\n");
	    t.printStackTrace();
	    System.exit(-1);
	}
			
	return topLevel;
    }

    
    public Object visitClassBody(JTypeDeclaration[] decls,
				 JFieldDeclaration[] fields,
                               JMethodDeclaration[] methods,
                               JPhylum[] body) 
    {
	blockStart("ClassBody");
	for (int i = 0; i < decls.length ; i++)
            trash = decls[i].accept(this);
	for (int i = 0; i < body.length ; i++)
            trash = body[i].accept(this);
	
	//force the init method to be visited first!
	for (int i = 0; i < methods.length; i++) {
	    if (methods[i].getName().equals("init")) {
		methods[i].accept(this);
		break;
	    }
	}
	    


        for (int i = 0; i < methods.length ; i++) {
	    if (!(methods[i].getName().equals("init")))
		trash = methods[i].accept(this); 
	}
     	return null;
    }

     /**
   * visits a class declaration
   */
    public Object visitInnerClassDeclaration(JClassDeclaration self,
                                           int modifiers,
                                           String ident,
                                           String superName,
                                           CClassType[] interfaces,
                                           JTypeDeclaration[] decls,
                                           JPhylum[] body,
					     JFieldDeclaration[] fields,
                                           JMethodDeclaration[] methods) 
    {
	printMe("visitInnerClassDeclaration");
        trash = visitClassBody(decls, fields, methods, body);
        return self;
    }
    

    //return the Ctype based on the string type
    private CType getType(String type) 
    {
	if (type.equals("Integer"))
	    return CStdType.Integer;
	else if (type.equals("Character"))
	    return CStdType.Char;
	else if (type.equals("Boolean"))
	    return CStdType.Boolean;
	else if (type.equals("Byte"))
	    return CStdType.Byte;
	else if (type.equals("Double"))
	    return CStdType.Double;
	else if (type.equals("Float"))
	    return CStdType.Float;
	else if (type.equals("Short"))
	    return CStdType.Short;
	else if (type.equals("String"))
	    return CStdType.String;	
	else if (type.equals("Long"))
	    return CStdType.Long;
	else
	    at.dms.util.Utils.fail("Non-Supported Type for Filter Input");
	return null;
    }

    // ----------------------------------------------------------------------
    // METHODS AND FIELDS
    // ----------------------------------------------------------------------

    /**
     * visits a field declaration
     */
    public Object visitFieldDeclaration(JFieldDeclaration self,
                                      int modifiers,
                                      CType type,
                                      String ident,
                                      JExpression expr) 
    {
	printMe("FieldDeclaration " + ident);
	/* Input declaration set the fields for the current
	   stream */ 
	if (CModifier.contains(CModifier.ACC_STATIC, modifiers))
	    at.dms.util.Utils.fail(printLine(self) +
				   "Cannot declare fields static");

	/* Output declaration set the fields for the current
	   stream */

	if (expr != null) {
	    Object o = expr.accept(this);
	    if (o instanceof JExpression) {
		expr = (JExpression)expr.accept(this);
	    } else {
		at.dms.util.Utils.fail(printLine(self) +
				       "Found a Channel declaration as a field -- new StreamIt syntax requires definition in init function instead.");
	    }
	}
	parentStream.addField(new JFieldDeclaration(null, new JVariableDefinition(null,
										  modifiers,
										  type,
										  ident,
										  expr),
						    null, null));
	return self;
    }


    private boolean ignoreMethodDeclaration(String ident) 
    {
	if (ident.equals("work") ||
	    ident.equals("add") ||
	    ident.equals("initIO") ||
	    ident.startsWith("initPath"))
	    return true;
	return false;
    }

    /**
     * visits a method declaration
     */
    public Object visitMethodDeclaration(JMethodDeclaration self,
                                       int modifiers,
                                       CType returnType,
                                       String ident,
                                       JFormalParameter[] parameters,
                                       CClassType[] exceptions,
                                       JBlock body) 
    {
        blockStart("MethodDeclaration: " + ident, self);
	//ignore main method
	if (ident.equals("main") && CModifier.contains(CModifier.ACC_STATIC, modifiers)
		 && CModifier.contains(CModifier.ACC_PUBLIC, modifiers)) {
	    printMe("Main ignored");
	    return self;
	}

	for (int i = 0; i < parameters.length; i++)
            trash = parameters[i].accept(this);
        
	body = (JBlock)body.accept(this);
	
	if (exceptions.length == 0)
	    exceptions = CClassType.EMPTY;

	/*Install work functio*/
	if (ident.equals("work")) {
	    if (parentStream instanceof SIRFilter) {
		((SIRFilter)parentStream).setWork(new JMethodDeclaration(null,
									 modifiers,
									 returnType,
									 ident,
									 parameters,
									 exceptions,
									 body,
									 null,
									 null));
	    }
	    else
		at.dms.util.Utils.fail(printLine(self) +
				       "Work Function Declared for Non-Filter");
	}
	
	/*Install init function for filter*/
	else if (ident.equals("init") && (parentStream instanceof SIRStream)) {
	    ((SIRStream)parentStream).setInit(new JMethodDeclaration(null,
								     modifiers,
								     returnType,
								     ident,
								     parameters,
								     exceptions,
								     body,
								     null,
								     null)); 
	}
	else if (ident.startsWith("initPath")) {
	    if (!(parentStream instanceof SIRFeedbackLoop))
		at.dms.util.Utils.fail(printLine(self) +
				       "initPath declared for non-Feedbackloop");
	    ((SIRFeedbackLoop)parentStream).setInitPath(new JMethodDeclaration(null,
									       modifiers,
									       returnType,
									       ident,
									       parameters,
									       exceptions,
									       body,
									       null,
									       null)); 
	}            
	else if (!ignoreMethodDeclaration(ident) && (parentStream instanceof SIRStream))
	    ((SIRStream)parentStream).addMethod(new JMethodDeclaration(null,
								       modifiers,
								       returnType,
								       ident,
								       parameters,
								       exceptions,
								       body,
								       null,
								       null));
	
	return self;
    }
    
    /**
     * visits a method declaration
     */
    public Object visitConstructorDeclaration(JConstructorDeclaration self,
                                            int modifiers,
                                            String ident,
                                            JFormalParameter[] parameters,
                                            CClassType[] exceptions,
                                            JConstructorBlock body) 
    {
        blockStart("ConstructorDeclaration", self);
	if (parameters.length == 0)
	    parameters = JFormalParameter.EMPTY;
	for (int i = 0; i < parameters.length; i++)
            trash = parameters[i].accept(this);
	trash = body.accept(this);
	if (exceptions.length == 0)
	    exceptions = CClassType.EMPTY;
	return self;
    }

    // ----------------------------------------------------------------------
    // STATEMENTS
    // ----------------------------------------------------------------------

    /**
     * visits a while statement
     */
    public Object visitWhileStatement(JWhileStatement self,
                                    JExpression cond,
                                    JStatement body) 
    {
        blockStart("WhileStatement", self);
        JExpression newCond = null;
	if (cond != null)
	    newCond = (JExpression)cond.accept(this);
        JStatement newBody = null;
	if (body != null)
	    newBody = (JStatement)body.accept(this);
	return new JWhileStatement(null, newCond, newBody, null);
    }

    /**
   * visits a variable declaration statement
   */
    public Object visitVariableDeclarationStatement
        (JVariableDeclarationStatement self,
         JVariableDefinition[] vars) 
    {
        blockStart("VariableDeclarationStatement", self);
        
	for (int i = 0; i < vars.length; i++) {
            vars[i] = (JVariableDefinition)vars[i].accept(this);	
	}
	for (int i = 0; i < vars.length; i++)
	    if (vars[i] != null)
		return new JVariableDeclarationStatement(null, vars, null);
	return null;
    }

    /**
     * visits a variable declaration statement
     */
    public Object visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) 
    {
        blockStart("VariableDefinition: " + self.getIdent(), self);
	printMe(type.toString());

	//The attribute returned from visiting the expression of the variable def
	Object retObj = null;
	//visit the expression
	if (expr != null)
	    retObj = expr.accept(this);

	//we are defining a new variable with a "new" in the code
	if (retObj instanceof JUnqualifiedInstanceCreation) {
	    //Portal creation (normal unqualified instance creation)
	    if (type.toString().endsWith("Portal")) {
		//Portal Definition , SIRCreatePortal add to symbol Table
		//make the SIRCreatePortal the new expression in the definition
		SIRCreatePortal cp = new SIRCreatePortal();
		return new JVariableDefinition(null, modifiers, type, ident, cp);
	    }
	    //defining a variable to be a named stream
	    //If the class of this variable is in the SIR table then
	    //we need to add this definition to the symbol table
	    else if (getVisitedOp(type.toString()) != null) {
		
		SIRStream ST = (SIRStream)ObjectDeepCloner.
		    deepCopy(getVisitedOp(type.toString()));
		printMe("Adding " + ident + " to symbol table");
		
		SIRInitStatement newSIRInit = 
		    new SIRInitStatement(null, 
					 null, 
					 ((JUnqualifiedInstanceCreation)retObj).
					 getParams(), 
					 ST);
		symbolTable.put(self, newSIRInit);
		return null;
		//return new JVariableDefinition(null, modifiers, type, ident, expr);
	    }
	    
	    else 
		at.dms.util.Utils.fail(printLine(self) +
				       "Definition of Stream " + type.toString() + 
				       " not seen before definition of " + ident);
	}
	// defining the var to be a anonymous class
	// visitUnqualifiedAnonymousCreation will return an SIRInitStatement
	else if (retObj instanceof SIRInitStatement) {
	    //just add the SIRInitStatement to the symbol table
	    printMe("Adding " + ident+ " to symbol table");
	    symbolTable.put(self, retObj);
	    return null;
	}

	return new JVariableDefinition(null, modifiers, type, ident, (JExpression)retObj);
    }
    
    /**
     * visits a switch statement
     */
    public Object visitSwitchStatement(JSwitchStatement self,
                                     JExpression expr,
                                     JSwitchGroup[] body) 
    {
        blockStart("SwitchStatement", self);
	if (expr != null)
	    expr = (JExpression)expr.accept(this);
        for (int i = 0; i < body.length; i++)
            body[i] = (JSwitchGroup)body[i].accept(this);
        return new JSwitchStatement(null, expr, body, null);
    }

    /**
     * visits a return statement
     */
    public Object visitReturnStatement(JReturnStatement self,
                                     JExpression expr) 
    {
        blockStart("Return", self);
        if (expr != null) 
	    expr = (JExpression)expr.accept(this);
        return new JReturnStatement(null, expr, null);
    }

    /**
     * visits a labeled statement
     */
    public Object visitLabeledStatement(JLabeledStatement self,
                                      String label,
                                      JStatement stmt) 
    {
        blockStart("LabeledStatement", self);
	if (stmt != null)
	    stmt = (JStatement)stmt.accept(this);
        return new JLabeledStatement(null, label, stmt, null);
    }

    /**
     * visits a if statement
     */
    public Object visitIfStatement(JIfStatement self,
                                 JExpression cond,
                                 JStatement thenClause,
                                 JStatement elseClause) 
    {
        blockStart("IfStatement", self);
	if (cond != null)
	    cond = (JExpression)cond.accept(this);
	if (thenClause != null)
	    thenClause = (JStatement)thenClause.accept(this);
        if (elseClause != null) 
	    elseClause = (JStatement)elseClause.accept(this);
        return new JIfStatement(null, cond, thenClause, elseClause, null);
    }

    /**
   * visits a for statement
   */
    public Object visitForStatement(JForStatement self,
                                  JStatement init,
                                  JExpression cond,
                                  JStatement incr,
                                  JStatement body) 
    {
        blockStart("ForStatement", self);
	if (body != null)
	    body = (JStatement)body.accept(this);
	if (init != null)
	    init = (JStatement)init.accept(this);
	if (cond != null)
	    cond = (JExpression)cond.accept(this);
	if (incr != null)
	    incr= (JStatement)incr.accept(this);	
	return new JForStatement(null, init, cond, incr, body, null);
    }

    /**
   * visits a compound statement
   */
    public Object visitCompoundStatement(JCompoundStatement self,
                                       JStatement[] body) 
    {
        blockStart("CompoundStatement", self);
        for (int i = 0; i < body.length; i++)
            body[i] = (JStatement)body[i].accept(this);
        return new JCompoundStatement(null, body);
    }

    /**
     * visits an expression statement
     */
    public Object visitExpressionStatement(JExpressionStatement self,
                                         JExpression expr) 
    {
        blockStart("ExpressionStatement", self);
	
	JPhylum attrib = null;
	if (expr != null)
	    attrib = (JPhylum)expr.accept(this);
	

	if (attrib instanceof JExpression)
	    return new JExpressionStatement(null, (JExpression)attrib, null);
	if (attrib == null)   //Used when we want to remove a statement
	    return null;
	else 
	    return attrib;
    }

    /**
     * visits an expression list statement
     */
    public Object visitExpressionListStatement(JExpressionListStatement self,
                                             JExpression[] expr) 
    {
        blockStart("ExpressionListStatement", self);
        for (int i = 0; i < expr.length; i++)
            expr[i] = (JExpression)expr[i].accept(this);
        return new JExpressionListStatement(null, expr, null);
    }

    /**
   * visits a empty statement
   */
    public Object visitEmptyStatement(JEmptyStatement self) 
    {
        blockStart("EmptyStatement", self);
        return self;
    }

    /**
   * visits a do statement
   */
    public Object visitDoStatement(JDoStatement self,
                                 JExpression cond,
                                 JStatement body) 
    {
        blockStart("DoStatement", self);
	if (cond != null) 
	    cond = (JExpression)cond.accept(this);
	if (body != null)
	    body = (JStatement)body.accept(this);
	return new JDoStatement(null, cond, body, null);
    }

    /**
     * visits a continue statement
     */
    public Object visitContinueStatement(JContinueStatement self,
                                       String label) 
    {
        blockStart("ContinueStatement", self);	
	return self;
    }

    /**
     * visits a break statement
     */
    public Object visitBreakStatement(JBreakStatement self,
                                    String label) 
    { 
        blockStart("BreakStatement", self);
	return self;
    }

    /**
     * visits an expression statement
     */
    public Object visitBlockStatement(JBlock self,
                                    JavaStyleComment[] comments) 
    {
	JStatement[] body = self.getStatementArray();
	JTryCatchStatement removeFlag = new JTryCatchStatement(null, null,
							       null, null);
	
	blockStart("BlockStatement", self);
        for (int i = 0; i < body.length; i++) {
	    Object st = body[i].accept(this);
	    if (st == null)
	    	body[i] = removeFlag;
	    else if (st instanceof JStatement)
		body[i] = (JStatement)st;
	}
	
	//Vector nullVector = new Vector(1);
	//nullVector.add(null);
	LinkedList bodyList = new LinkedList(Arrays.asList(body));
	while (bodyList.remove(removeFlag));
	
	JStatement[] newBody = new JStatement[bodyList.size()];
	for (int i = 0; i< bodyList.size(); i++)
	    newBody[i] = (JStatement)bodyList.get(i);
	
	return new JBlock(null, newBody, null);
	//return new JBlock (null, body, null);
	
    }

    /**
     * visits a type declaration statement
     */
    public Object visitTypeDeclarationStatement(JTypeDeclarationStatement self,
						JTypeDeclaration decl) 
    {
        blockStart("TypeDeclarationStatement", self);
        if (decl != null)
	    trash = decl.accept(this);
	return self;
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * visits an unary plus expression
     */
    public Object visitUnaryPlusExpression(JUnaryExpression self,
                                         JExpression expr) 
    {
        blockStart("UnaryPlusExpression", self);
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));   
	return self;
    }

    /**
     * visits an unary minus expression
     */
    public Object visitUnaryMinusExpression(JUnaryExpression self,
                                          JExpression expr) 
    {
        blockStart("UnaryMinusExpression", self);
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));   
	return self;
    }

    /**
     * visits a bitwise complement expression
     */
    public Object visitBitwiseComplementExpression(JUnaryExpression self,
                                                 JExpression expr) 
    {
        blockStart("BitwiseComplementExpression", self);
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));   
	return self;
    }

    /**
     * visits a logical complement expression
     */
    public Object visitLogicalComplementExpression(JUnaryExpression self,
                                                 JExpression expr) 
    {
        blockStart("LogicalComplementExpression", self);
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));   
	return self;
    }

    /**
     * visits a type name expression
     */
    public Object visitTypeNameExpression(JTypeNameExpression self,
                                        CType type) 
    {
        blockStart("TypeNameExpression", self);
	return self;
    }

    /**
     * visits a this expression
     */
    public Object visitThisExpression(JThisExpression self,
                                    JExpression prefix) 
    {
        blockStart("ThisExpression", self);
	//never need to visit a this expression
	return self;
	/*if (prefix != null) {
	    prefix = (JExpression)prefix.accept(this);
	    }
	return new JThisExpression(null, prefix);
	*/
    }

    /**
     * visits a super expression
     */
    public Object visitSuperExpression(JSuperExpression self) 
    {
        blockStart("SuperExpression", self);
	return self;
    }

    /**
     * visits a shift expression
     */
    public Object visitShiftExpression(JShiftExpression self,
                                     int oper,
                                     JExpression left,
                                     JExpression right) 
    {
        blockStart("ShiftExpression", self);
	if (left != null)
	    self.setLeft((JExpression)left.accept(this));
	if (right != null)
	    self.setRight((JExpression)right.accept(this));
	return self;
    }

    /**
     * visits a shift expressiona
     */
    public Object visitRelationalExpression(JRelationalExpression self,
                                          int oper,
                                          JExpression left,
                                          JExpression right) 
    {
        blockStart("RelationalExpression", self);
	if (left != null)
	    self.setLeft((JExpression)left.accept(this));
	if (right != null)
	    self.setRight((JExpression)right.accept(this));
	return self;
    }

    /**
     * visits a prefix expression
     */
    public Object visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr) 
    {
        blockStart("PrefixExpression", self);
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));
	return self;
    }

    /**
     * visits a postfix expression
     */
    public Object visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr) 
    {
        blockStart("PostfixExpression", self);
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));
	return self;
    }

    /**
     * visits a parenthesed expression
     */
    public Object visitParenthesedExpression(JParenthesedExpression self,
                                           JExpression expr) 
    {
        // Not parenthesized?
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));
	return self;
    }

    /**
     * Visits an unqualified anonymous class instance creation expression.
     */
    public Object visitQualifiedAnonymousCreation
        (JQualifiedAnonymousCreation self,
         JExpression prefix,
         String ident,
         JExpression[] params,
         JClassDeclaration decl) 
    {
        blockStart("QualifiedAnonymousCreation", self);
	if (prefix != null)
	    prefix = (JExpression)prefix.accept(this);
	for (int i = 0; i < params.length; i++)
            params[i] = (JExpression)params[i].accept(this);
	for (int i = 0; i < decl.fields.length; i++) {
	    decl = (JClassDeclaration)decl.fields[i].variable.accept(this); 
	}
	return new JQualifiedAnonymousCreation(null, prefix, ident, params, decl);
    }

    /**
     * Visits an unqualified instance creation expression.
     */
    public Object visitQualifiedInstanceCreation(JQualifiedInstanceCreation self,
                                               JExpression prefix,
                                               String ident,
                                               JExpression[] params) 
    {
        blockStart("QualifiedInstanceCreation", self);
	if (prefix != null)
	    prefix = (JExpression)prefix.accept(this);
	for (int i = 0; i < params.length; i++)
            params[i] = (JExpression)params[i].accept(this);
	return new JQualifiedInstanceCreation(null, prefix, ident, params);
    }
    
    /**
     * Visits an unqualified anonymous class instance creation expression.
     */
    public Object visitUnqualifiedAnonymousCreation
        (JUnqualifiedAnonymousCreation self,
         CClassType type,
         JExpression[] params,
         JClassDeclaration decl) 
    {
        blockStart("UnqualifiedAnonymousCreation", self);
	//Get the SIR Operator back from the Class Declaration
	SIROperator SIROp = (SIROperator)decl.accept(this);
	if (!(SIROp instanceof SIRStream))
	    at.dms.util.Utils.fail(printLine(self) +
				   "Trying to anonymously create non-Stream");
     	for (int i = 0; i < params.length; i++)
            params[i] = (JExpression)params[i].accept(this);
	if (params.length == 0)
	    trash = params = JExpression.EMPTY;
	      /*	
			for (int i = 0; i < decl.fields.length; i++) {
			trash = decl.fields[i].variable.accept(this); 
			}
	      */
	//The only time that we can use an Anonymous class is inside an add statement 
	//which needs to be translated into an SIRInitStatement
	SIRInitStatement sis = new SIRInitStatement(null, null, params, (SIRStream)SIROp);
	return sis;
    }

    /**
     * Visits an unqualified instance creation expression.
     */
    public Object visitUnqualifiedInstanceCreation
        (JUnqualifiedInstanceCreation self,
         CClassType type,
         JExpression[] params) 
    {
        blockStart("UnqualifiedInstanceCreation", self);
	printMe("  Declaring: " + type.getIdent());
	if (params.length == 0)
	    params = JExpression.EMPTY;
	if (type.getIdent().equals("Channel")) 
	    /*Channel declaration, treat the args as special */
	{
	    Vector v = new Vector(3);
	    v.add((String)params[0].accept(this));
	    v.add((JExpression)params[1].accept(this));
	    if (params.length > 2) { 
		v.add((JExpression)params[2].accept(this));
	    }
	    return v;
	}
	else {     /* Not a channel */
	    for (int i = 0; i < params.length; i++) 
		params[i] = (JExpression)params[i].accept(this);
	}
	return new JUnqualifiedInstanceCreation(null, type, params);
    }

    /**
     * visits an array allocator expression
     */
    public Object visitNewArrayExpression(JNewArrayExpression self,
                                        CType type,
                                        JExpression[] dims,
                                        JArrayInitializer init) 
    {
        blockStart("NewArrayExpression", self);
	if (dims.length == 0)
	    dims = JExpression.EMPTY;
	for (int i = 0; i < dims.length; i++)
            dims[i] = (JExpression)dims[i].accept(this);
	return new JNewArrayExpression(null, type, dims, init);
    }

    /**
     * visits a name expression
     */
    public Object visitNameExpression(JNameExpression self,
                                    JExpression prefix,
                                    String ident) 
    {
        blockStart("NameExpression", self);
	if (prefix != null)
	    self.setPrefix((JExpression)prefix.accept(this));
	return self;
    }

    /**
     * visits an array allocator expression
     */
    public Object visitBinaryExpression(JBinaryExpression self,
                                      String oper,
                                      JExpression left,
                                      JExpression right) 
    {
	blockStart("BinaryExpression", self);
	if (left != null)
	    self.setLeft((JExpression)left.accept(this));
	if (right != null)
	    self.setRight((JExpression)right.accept(this));
	return self;
    }


    private SIRMessageStatement createMessageStatement(JMethodCallExpression methCall,
						       CType portal, 
						       JExpression prefix, 
						       JExpression[] args)  
    {
	//index of the method in the portal interface
	int index = -1;
	//Process the args
	for (int i = 0; i < args.length; i++)
	    args[i] = (JExpression) args[i].accept(this);

	//Find the index of the method call
	CClassType[] interfaces = 
	    portal.getCClass().getInterfaces();
	
	if (interfaces.length > 1)
	    at.dms.util.Utils.fail(printLine(methCall) +
				   "A portal can only implement one interface at this time");
	
	//Get the portal methods of the portal Interface
        // Save this code; we're not using it now, but it might be useful
        // for finding the right interface, if/when multi-interface
        // portals happen.
        /*
	CMethod[] portalMethods = interfaces[0].getCClass().getMethods();

	for (int i = 0; i < portalMethods.length; i++) {
	    if (portalMethods[i].getIdent().equals(methCall.getIdent())) {
		index = i;
		break;
	    }
	}
	if (index == -1)  //cannot find method
	    at.dms.util.Utils.fail("Cannot find portal method " + methCall.getIdent() + " in portal interface");
        */
        String interfaceName = interfaces[0].getIdent();
        
	//Assuming all messages best effort
	return new SIRMessageStatement(prefix, interfaceName, methCall.getIdent(), args, SIRLatency.BEST_EFFORT);
    }
		

    //given a method call, a JLocalVariableExpression portal, and a stream, build the 
    //SIRRegReceiveStatement
    private SIRRegReceiverStatement createRegReceiver(JMethodCallExpression methCall,
						      CType portal,
						      JExpression prefix,
						      SIRStream st) 
    {
	CMethod meth = methCall.getMethod();
	//the index of the method in the interface
	
	//Extract the interface from the portal prefix variable
	CClassType[] interfaces = 
	    portal.getCClass().getInterfaces();
	
	if (interfaces.length > 1)
	    at.dms.util.Utils.fail(printLine(methCall) +
				   "A portal can only implement one interface at this time");
	
	//Get the portal methods of the portal Interface
	CMethod[] portalMethods = interfaces[0].getCClass().getMethods();
	
	//get the CMethods from the stream class
	JMethodDeclaration[] streamMethodDecs = st.getMethods();
	
	//Store the methods in the order of the interface
	JMethodDeclaration[] matchedMethods = new JMethodDeclaration[portalMethods.length];
	
	//Build matchedMethods by iterating over all the methods in the portal interface
	//and matching them with the methods of the stream class
	for (int i = 0; i < portalMethods.length; i++) {
	    JMethodDeclaration currentMethod = null;
	    for (int j = 0; j < streamMethodDecs.length; j++) {
		if (portalMethods[i].getIdent().equals(streamMethodDecs[j].getName())) {
		    currentMethod = streamMethodDecs[j];
		    break;
		}
	    }
	    if (currentMethod == null)
		at.dms.util.Utils.fail(printLine(methCall) +
				       "Cannot find portal method in portal interface (RegReceiver)");
	    matchedMethods[i] = currentMethod;
	}
	
	// Keep track of the list of methods for this interface with a 
	// interface table, to be passed on with a callback to Kopi2SIR
        SIRInterfaceTable itable =
            new SIRInterfaceTable(null, 
                                  interfaces[0], 
                                  (JMethodDeclaration[])
                                  matchedMethods.clone());
	interfaceTableList.add(itable);
	return new SIRRegReceiverStatement(prefix,st, itable);
    }

		      


    //This method creates an initStatement from the underlying arguments
    //to the function should be translated into an init statment
    //also, based on the String argument, it registers the SIROp
    //with the Stream
    private SIRInitStatement createInitStatement(Object SIROp, String regMethod) 
    {
	SIRStream newST = null;
	/*
	  //Already an SIRinit statement
	  if (SIROp instanceof SIRInitStatement) {
	  return (SIRInitStatement)SIROp;
	  }
	  //Using a local variable that has been already defined as a stream construct
	  if (SIROp instanceof SIRStream)
	    newST = (SIRStream)SIROp;
	*/


	// Already an SIRinit statement (this is a local variable expression or a anonymous
	// creation)
	// we use the init statement from the symbol table (for a local var) or created it in the 
	// or the one created in the AnonymousCreation
	if (SIROp instanceof SIRInitStatement) {
	    //now we must set the parent and do any other steps that are necessary 
	    // when registering
	    newST = (SIRStream)((SIRInitStatement)SIROp).getTarget();
	    newST.setParent((SIRContainer)parentStream);
	}

	if (SIROp instanceof SIRStream)
	    newST = (SIRStream)SIROp;

	//Creating a named class
	//if it is a named creation, lookup in the symbol table for the visited
	//node and add it to the pipeline
	if (SIROp instanceof JUnqualifiedInstanceCreation) {
	    SIRStream st = (SIRStream)getVisitedOp(((JUnqualifiedInstanceCreation)SIROp).
						   getType().getCClass().getIdent());
	    
	    //if we cannot find the stream in the symbol table then 
	    //we have not seen the declaration before the use
	    if (st == null)
		at.dms.util.Utils.fail(lineNumber + 
				       ": cannot find declaration of stream " +
				       ((JUnqualifiedInstanceCreation)SIROp).
				       getType().getCClass().getIdent());
	    newST = (SIRStream) ObjectDeepCloner.deepCopy(st);
	    //SIRStream newST = (SIRStream) st.clone();
	    newST.setParent((SIRContainer)parentStream);
	}
	//Die if it is none of the above case
	if (newST == null)
	    at.dms.util.Utils.fail(
				   "Illegal Arg to Stream Add Construct");
	
	//now find the registration method
	if (regMethod.equals("add")) {
	    if (parentStream instanceof SIRPipeline){
		((SIRPipeline)parentStream).add(newST);
	    }
	    else {
		((SIRSplitJoin)parentStream).add(newST);
	    }
	}
	else if (regMethod.equals("setBody"))
	    ((SIRFeedbackLoop)parentStream).setBody(newST);
	else if (regMethod.equals("setLoop"))
	    ((SIRFeedbackLoop)parentStream).setLoop(newST);
	else
	    at.dms.util.Utils.fail(lineNumber + ": Invalid Registration Method");

	//Already an SIRinit statement (this is a local variable expression)
	//we used the init statement from the symbol table
	//Using a local variable that has been already defined as a stream construct
	if (SIROp instanceof SIRInitStatement) 
	    	    return (SIRInitStatement)SIROp;
	else 
	    return new SIRInitStatement(null, null, 
					((JUnqualifiedInstanceCreation)SIROp).getParams(),
					newST);
    }
    
    /**
     * visits a method call expression
     */
    public Object visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) 
    {
	//Save method we were processing
	String parentMethod = currentMethod;
	blockStart("MethodCallExpression: " + self.getIdent(), self);
        //Set currentMethod to this method, we are processing it!
	currentMethod = self.getIdent();
	
	
	if (isSIRExp(self)) {
	    printMe("SIR Expression " + ident);
	    //reset currentMethod on all returns
	    currentMethod = parentMethod;
	    return newSIRExp(self, args);
	}
	else if (ident.equals("regReceiver")) { 
	    if (args.length > 1)
		at.dms.util.Utils.fail(printLine(self) + 
				       "Exactly one arg to add() allowed");
	    SIRStream st = ((SIRInitStatement)args[0].accept(this)).getTarget();
	    currentMethod = parentMethod;
	    //prefix can either be a field or a local var, extract type
	    if (prefix instanceof JLocalVariableExpression)
		return createRegReceiver(self, ((JLocalVariableExpression)prefix).getVariable().getType(),
					 prefix, st);
	    else if (prefix instanceof JFieldAccessExpression)
		return createRegReceiver(self, ((JFieldAccessExpression)prefix).getType(),
					 prefix, st);
	    else
		at.dms.util.Utils.fail(printLine(self) + 
				       "regReceiver all must have a portal prefix");
	    return null;
	}
	else if (ident.equals("add")) {            //Handle an add call in a pipeline
	    //Parent must be a pipeline
	    if (!((parentStream instanceof SIRPipeline) || parentStream instanceof SIRSplitJoin)) 
		at.dms.util.Utils.fail(printLine(self) + 
				       "Add not called on Pipeline or SplitJoin");
	    if (args.length > 1)
		at.dms.util.Utils.fail(printLine(self) + 
				       "Exactly one arg to add() allowed");
	    //Visit the argument (Exactly one))
	    Object SIROp = args[0].accept(this);
	    	    
	    //reset currentMethod on all returns
	    currentMethod = parentMethod;
	    //create the init statement to return
	    return createInitStatement(SIROp, ident);
	    
	} else if (ident.equals("setDelay")) {
	    if (!(parentStream instanceof SIRFeedbackLoop))
		at.dms.util.Utils.fail(printLine(self) + 
				       "SetDelay called on Non-FeedbackLoop");
	    if (args.length > 1)
		at.dms.util.Utils.fail(printLine(self) + 
				       "Too many args to setDelay");
	    int delay  = ((JIntLiteral)args[0].accept(this)).intValue();
	    ((SIRFeedbackLoop)parentStream).setDelay(delay);
	    //reset currentMethod on all returns
	    currentMethod = parentMethod;
	    //we want to ignore remove this method from the block
	    return null;
	}
	else if (ident.equals("setBody")) {
	    if (!(parentStream instanceof SIRFeedbackLoop)) 
		at.dms.util.Utils.fail(printLine(self) + 
				       "setBody called on non-FeedbackLoop");
	    if (args.length > 1)
		at.dms.util.Utils.fail(printLine(self) + 
				       "Exactly one arg to setBody() allowed");
	    //Visited the argument
	    Object SIROp = args[0].accept(this);
	    //reset currentMethod on all returns
	    currentMethod = parentMethod;
	    //create the init statement to return
	    return createInitStatement(SIROp, ident);
	}
	else if (ident.equals("setLoop")) {
	    if (!(parentStream instanceof SIRFeedbackLoop)) 
		at.dms.util.Utils.fail(printLine(self) + 
				       "setLoop called on non-FeedbackLoop");
	    if (args.length > 1)
		at.dms.util.Utils.fail(printLine(self) + 
				       "Exactly one arg to setLoop() allowed");
	    //Visited the argument
	    Object SIROp = args[0].accept(this);
	     //reset currentMethod on all returns
	    currentMethod = parentMethod;
	    //create the init statement to return
	    return createInitStatement(SIROp, ident);
	}
       
	else if (ident.equals("setSplitter")) {
	    //we build the splitter here and set the global splitType
	    //then we set the splitter in the postVisit of the stream
	    buildSplitter(args[0]);
	    currentMethod = parentMethod;
	    return null;
	}
	else if (ident.equals("setJoiner")) {
	    //we build the joiner here and set the global joinType
	    //then we set the joiner in the postVisit of the stream
	    buildJoiner(args[0]);
	    currentMethod = parentMethod;
	    //we want to ignore remove this method from the block
	     return null;
	}
	//There are two cases for portal calls
	//Message send to a portal using a local var to store the portal
	//I think that this is the only case when prefix will not be null??? Keep the check any way
	else if (prefix instanceof JLocalVariableExpression &&
		 ((JLocalVariableExpression)prefix).getVariable().getType().toString().endsWith("Portal")) {
	    //check if this is a regSender, if so ignore it
	    if (ident.equals("regSender"))
		return null;
	    return createMessageStatement(self, ((JLocalVariableExpression)prefix).getVariable().getType(), 
					  prefix, args);
	}
	//message send to portal using field as portal
	else if (prefix instanceof JFieldAccessExpression &&
		 ((JFieldAccessExpression)prefix).getType().toString().endsWith("Portal")) {
	    if (ident.equals("regSender"))
		return null;
	    return createMessageStatement(self, ((JFieldAccessExpression)prefix).getType(), prefix, args);
	}	    
      	else {             //Not an SIR call
	    prefix = (JExpression)prefix.accept(this);
	    for (int i = 0; i < args.length; i++)
		args[i] = (JExpression) args[i].accept(this);
	    //reset currentMethod
	    currentMethod = parentMethod;
	    self.setArgs(args);
	    self.setPrefix(prefix);
	    return self;
	}

    }
    

    private void buildSplitter(JExpression type) 
    {
	if (!(type instanceof JMethodCallExpression))
	    at.dms.util.Utils.fail(printLine(type) + 
				   "arg to setSplitter must be a method call");

	JMethodCallExpression splitter = (JMethodCallExpression)type;
	
	if (splitter.getIdent().equals("ROUND_ROBIN"))
	    splitType = SIRSplitType.ROUND_ROBIN;
	else if (splitter.getIdent().equals("DUPLICATE"))
	    splitType = SIRSplitType.DUPLICATE;
	else if (splitter.getIdent().equals("NULL"))
	    splitType = SIRSplitType.NULL;
	else if (splitter.getIdent().equals("WEIGHTED_ROUND_ROBIN")) {
	    //For weighted round robins, the splitter get built here and set here

	    JExpression[] args = splitter.getArgs();
	    if (parentStream instanceof SIRSplitJoin)
		((SIRSplitJoin)parentStream).setSplitter(SIRSplitter.
							createWeightedRR((SIRSplitJoin)
									 parentStream, 
									 args));
	    if (parentStream instanceof SIRFeedbackLoop)
		((SIRFeedbackLoop)parentStream).setSplitter(SIRSplitter.
							 createWeightedRR((SIRFeedbackLoop)
									  parentStream, 
									  args));
	    splitType = null;
	}
	else
	    at.dms.util.Utils.fail(printLine(type) + 
				   "Unsupported Split Type");
	
    }

    private void buildJoiner(JExpression type) 
    {
	if (!(type instanceof JMethodCallExpression))
	    at.dms.util.Utils.fail(printLine(type) + 
				   "arg to setJoiner must be a method call");

	JMethodCallExpression joiner = (JMethodCallExpression)type;
	
	if (joiner.getIdent().equals("ROUND_ROBIN"))
	    joinType = SIRJoinType.ROUND_ROBIN;
	else if (joiner.getIdent().equals("COMBINE"))
	    joinType = SIRJoinType.COMBINE;	  	
	else if (joiner.getIdent().equals("NULL"))
		joinType = SIRJoinType.COMBINE;
	else if (joiner.getIdent().equals("WEIGHTED_ROUND_ROBIN")) {
	    //For weighted round robins, the joiner get built here and set here

	    JExpression[] args = joiner.getArgs();
	    if (parentStream instanceof SIRSplitJoin)
		((SIRSplitJoin)parentStream).setJoiner(SIRJoiner.
							createWeightedRR((SIRSplitJoin)
									 parentStream, 
									 args));
	    if (parentStream instanceof SIRFeedbackLoop)
		((SIRFeedbackLoop)parentStream).setJoiner(SIRJoiner.
							 createWeightedRR((SIRFeedbackLoop)
									  parentStream, 
									  args));
	    joinType = null;
	}
	
	    

    }
    
    
    

    /**
   * visits a local variable expression
   */
    public Object visitLocalVariableExpression(JLocalVariableExpression self,
                                             String ident) 
    {
        blockStart("LocalVariableExpression", self);
	//Lookup the current local variable in the symbol table
	//if it exists then this is a variable of a streamit type
	if (symbolTable.get(self.getVariable()) != null) {
	    printMe("Using symbol table entry for " + ident);
	    return symbolTable.get(self.getVariable());
	}
	return self;
    }

  
    /**
     * visits an equality expression
     */
    public Object visitEqualityExpression(JEqualityExpression self,
                                        boolean equal,
                                        JExpression left,
                                        JExpression right) 
    {
        blockStart("EqualityExpression", self);
	if (left != null)
	    self.setLeft((JExpression)left.accept(this));
	if (right != null)
	    self.setRight((JExpression)right.accept(this));
	return self;
    }

    /**
     * visits a conditional expression
     */
    public Object visitConditionalExpression(JConditionalExpression self,
                                           JExpression cond,
                                           JExpression left,
                                           JExpression right) 
    {
        blockStart("ConditionalExpression", self);
	if (cond != null)
	    self.setCond((JExpression)cond.accept(this));
	if (left != null)
	    self.setLeft((JExpression)left.accept(this));
	if (right != null)
	    self.setRight((JExpression)right.accept(this));
	return self;
    }

    /**
     * visits a compound expression
     */
    public Object visitCompoundAssignmentExpression
        (JCompoundAssignmentExpression self,
         int oper,
         JExpression left,
         JExpression right) 
    {
        blockStart("CompoundAssignmentExpression", self);
	if (left != null)
	    self.setLeft((JExpression)left.accept(this));
	if (right != null)
	    self.setRight((JExpression)right.accept(this));
	return self;
    }

    /**
     * visits a field expression
     */
    public Object visitFieldExpression(JFieldAccessExpression self,
                                     JExpression left,
                                     String ident) 
    {
        blockStart("FieldExpression", self);
	//return a string if this is a field expression that accesses the type
	//argument in a channel instaniation
	if (supportedType(left.getType().getCClass().getIdent())) {
	    return left.getType().getCClass().getIdent();
	}
	//Never need to visit a field expression
	return self;
    }

    /**
   * visits a class expression
   */
    public Object visitClassExpression(JClassExpression self,
                                     CType type) 
    {
        blockStart("ClassExpression", self);
	return self;
    }

    /**
     * visits a cast expression
     */
    public Object visitCastExpression(JCastExpression self,
                                    JExpression expr,
                                    CType type) 
    {
        blockStart("CastExpression", self);
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));
	return self;
    }

    /**
     * visits a cast expression
     */
    public Object visitUnaryPromoteExpression(JUnaryPromote self,
                                            JExpression expr,
                                            CType type) 
    {
        blockStart("UnaryPromoteExpression", self);
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));
	return self;
    }

    /**
     * visits a compound assignment expression
     */
    public Object visitBitwiseExpression(JBitwiseExpression self,
                                       int oper,
                                       JExpression left,
                                       JExpression right) 
    {
        blockStart("BitwiseExpression", self);
	if (left != null)
	    self.setLeft((JExpression)left.accept(this));
	if (right != null)
	    self.setRight((JExpression)right.accept(this));
	return self;
    }
    
    /**
     * visits an assignment expression
     */
    public Object visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right) 
    {
        blockStart("AssignmentExpression", self);
	
	//must check for an assignment to input or output inside of
	//an init statement...
	if (left instanceof JFieldAccessExpression) {
	    if (((JFieldAccessExpression)left).getIdent().equals("input")) {
		if (!(parentStream instanceof SIRFilter))
		    at.dms.util.Utils.fail(printLine(self) + 
					   "Input declaration on non-Filter");
		SIRFilter filter = (SIRFilter)parentStream;
		Vector v = (Vector)right.accept(this);
		filter.setInputType(getType((String)v.elementAt(0)));
		filter.setPop((JExpression)v.elementAt(1));
		//If a peek value is given, and it is greater than pops
		//set the peek
		if (v.size() > 2) {
		    filter.setPeek((JExpression)v.elementAt(2));
		}
		else  //Otherwise set the peeks to the number of pops
		    filter.setPeek((JExpression)v.elementAt(1));
		return null;
	    }
       	    else if (((JFieldAccessExpression)left).getIdent().equals("output")) {
		if (!(parentStream instanceof SIRFilter))
		    at.dms.util.Utils.fail(printLine(self) + 
					   "Output declaration on non-Filter");
		SIRFilter filter = (SIRFilter)parentStream;
		Vector v = (Vector)right.accept(this);
		filter.setPush((JExpression)v.elementAt(1));
		filter.setOutputType(getType((String)v.elementAt(0)));
		return null;
	    }

	}
	if (left != null)
	    self.setLeft((JExpression)left.accept(this));
	if (right != null)
	    self.setRight((JExpression)right.accept(this));
	return self;
    }

    /**
     * visits an array length expression
     */
    public Object visitArrayLengthExpression(JArrayLengthExpression self,
                                           JExpression prefix) 
    {
        blockStart("ArrayLengthExpression", self);
	if (prefix != null)
	    prefix = (JExpression)prefix.accept(this);
        return new JArrayLengthExpression(null, prefix);
    }

    /**
     * visits an array length expression
     */
    public Object visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor) 
    {
        blockStart("ArrayAccessExpression", self);
	if (prefix != null)
	    self.setPrefix((JExpression)prefix.accept(this));
	if (accessor != null)
	    self.setAccessor((JExpression)accessor.accept(this));
	return self;
    }

  
    // ----------------------------------------------------------------------
    // OTHERS
    // ----------------------------------------------------------------------

    /**
     * visits an array length expression
     */
    public Object visitSwitchLabel(JSwitchLabel self,
                                 JExpression expr) 
    {
        blockStart("SwitchLabel", self);
	if (expr != null)
	    expr = (JExpression)expr.accept(this);
        return new JSwitchLabel(null, expr);
    }

    /**
     * visits an array length expression
     */
    public Object visitSwitchGroup(JSwitchGroup self,
                                 JSwitchLabel[] labels,
                                 JStatement[] stmts) 
    {
        blockStart("SwitchGroup", self);
	for (int i = 0; i < labels.length; i++)
            labels[i] = (JSwitchLabel)labels[i].accept(this);
	for (int i = 0; i < stmts.length; i++)
            stmts[i] = (JStatement)stmts[i].accept(this);
	return new JSwitchGroup(null, labels, stmts);
    }

  
    /**
     * visits an array length expression
     */
    public Object visitFormalParameters(JFormalParameter self,
                                      boolean isFinal,
                                      CType type,
                                      String ident) 
    {
        blockStart("FormalParameter", self);
	return self;
    }

    /**
     * visits an array length expression
     */
    public Object visitConstructorCall(JConstructorCall self,
                                     boolean functorIsThis,
                                     JExpression[] params) 
    {
        blockStart("ConstructorCall", self);
	if (params.length == 0)
	    params = JExpression.EMPTY;
	for (int i = 0; i < params.length; i++)
            params[i] = (JExpression)params[i].accept(this);
	return new JConstructorCall(null, functorIsThis, params);
    }

    /**
     * visits an array initializer expression
     */
    public Object visitArrayInitializer(JArrayInitializer self,
                                      JExpression[] elems) 
    {
        blockStart("ArrayInitializer", self);
        for (int i = 0; i < elems.length; i++)
            elems[i] = (JExpression)elems[i].accept(this);
        return new JArrayInitializer(null, elems);
    }

    /**
     * visits a boolean literal
     */
  public Object visitBooleanLiteral(JBooleanLiteral self,
			     boolean value) 
    {
        blockStart("BooleanLiteral", self);
	return new JBooleanLiteral(null, value);
    }

    /**
     * visits a byte literal
     */
  public Object visitByteLiteral(JByteLiteral self,
			  byte value) 
    {
        blockStart("ByteLiteral", self);
	return new JByteLiteral(null, value);
    }

    /**
     * visits a character literal
     */
  public Object visitCharLiteral(JCharLiteral self,
			  char value) 
    {
        blockStart("CharLiteral", self);
	return new JCharLiteral(null, value);
    }

    /**
     * visits a double literal
     */
  public Object visitDoubleLiteral(JDoubleLiteral self,
			    double value) 
    {
        blockStart("DoubleLiteral", self);
	return new JDoubleLiteral(null, value);
    }

    /**
     * visits a float literal
     */
  public Object visitFloatLiteral(JFloatLiteral self,
			   float value) 
    {
        blockStart("FloatLiteral", self);
	return new JFloatLiteral(null, value);
    }

    /**
     * visits a int literal
     */
  public Object visitIntLiteral(JIntLiteral self,
			 int value) 
    {
        blockStart("IntLiteral", self);
	return new JIntLiteral(null,value);
    }

    /**
     * visits a long literal
     */
  public Object visitLongLiteral(JLongLiteral self,
			  long value) 
    {
        blockStart("LongLiteral", self);
	return new JLongLiteral(null, value);
    }

    /**
     * visits a short literal
     */
  public Object visitShortLiteral(JShortLiteral self,
			   short value) 
    {
        blockStart("ShortLiteral", self);
	return new JShortLiteral(null, value);
    }

    /**
     * visits a string literal
     */
  public Object visitStringLiteral(JStringLiteral self,
			    String value) 
    {
        blockStart("StringLiteral", self);
	return new JStringLiteral(null, value);
    }

    /**
     * visits a null literal
     */
  public Object visitNullLiteral(JNullLiteral self) 
    {
        blockStart("NullLiteral", self);
	return new JNullLiteral(null);
    }

    /*-------------------------------------------------------------------------
      UNUSED (IN STREAMIT) VISITORS 
      -------------------------------------------------------------------------*/

    /**
     * visits a package name declaration
     */
    public Object visitPackageName(String name) 
    {
        blockStart("PackageName");
	return null;
    }

    /**
     * visits a package import declaration
     */
    public Object visitPackageImport(String name) 
    {
        blockStart("PackageImport");
	return null;
    }

    /**
     * visits a class import declaration
     */
    public Object visitClassImport(String name) 
    {
        blockStart("ClassImport");
	return null;
    }

    /**
     * Returns a vector of all the JInterfaceDeclarations for the
     * toplevel stream
     */
    public JInterfaceDeclaration[] getInterfaces()  {
	JInterfaceDeclaration[] ret = (JInterfaceDeclaration[])interfaceList.toArray(new JInterfaceDeclaration[0]);
	return ret;
    }
    
    /**
     * Returns a vector of all the SIRInterfaceTables that were
     * constructed in traversing the toplevel stream
     */
    public SIRInterfaceTable[] getInterfaceTables()  {
	SIRInterfaceTable[] ret = (SIRInterfaceTable[])interfaceTableList.toArray(new SIRInterfaceTable[0]);
	return ret;
    }
    

    /**
     * visits an interface declaration
     */
      public Object visitInterfaceDeclaration(JInterfaceDeclaration self,
                                          int modifiers,
                                          String ident,
                                          CClassType[] interfaces,
                                          JPhylum[] body,
                                          JMethodDeclaration[] methods) 
    {
	blockStart("visitInterfaceDeclaration", self);
	interfaceList.add(self);
	/*visitClassBody(new JTypeDeclaration[0], methods, body);*/
	return self;
    }
  
    /**
     * visits a try-catch statement
     */
    public Object visitTryCatchStatement(JTryCatchStatement self,
                                       JBlock tryClause,
                                       JCatchClause[] catchClauses) 
    {
        blockStart("TryCatchStatement", self);
        /*tryClause.accept(this);
	  for (int i = 0; i < catchClauses.length; i++)
	  catchClauses[i].accept(this);*/  
	return self;
    }


    /**
     * visits a try-finally statement
     */
    public Object visitTryFinallyStatement(JTryFinallyStatement self,
                                         JBlock tryClause,
                                         JBlock finallyClause) 
    {
        blockStart("TryFinallyStatement", self);
	/* tryClause.accept(this);
	   finallyClause.accept(this);*/
        return self;
    }

    /**
     * visits a throw statement
     */
    public Object visitThrowStatement(JThrowStatement self,
                                    JExpression expr) 
    {
        blockStart("ThrowStatement", self);
	/*expr.accept(this);*/
        return self;
    }

    /**
     * visits a synchronized statement
     */
    public Object visitSynchronizedStatement(JSynchronizedStatement self,
                                           JExpression cond,
                                           JStatement body) 
    {
        blockStart("SynchronizedStatement", self);
        /*cond.accept(this);
	  body.accept(this);*/
        return self;
    }


      /**
   * visits an instanceof expression
   */
    public Object visitInstanceofExpression(JInstanceofExpression self,
                                          JExpression expr,
                                          CType dest) 
    {
        blockStart("InstanceOfExpression", self);
	trash = expr.accept(this);
	return self;
    }

      /**
     * visits an array length expression
     */
    public Object visitComments(JavaStyleComment[] comments)  
    {
	blockStart("Comments");
	return comments;
    }

    /**
     * visits an array length expression
     */
    public Object visitComment(JavaStyleComment comment)  
    {
	blockStart("Comment");
	return comment;
    }

    /**
     * visits an array length expression
     */
    public Object visitJavadoc(JavadocComment comment) 
    {
	blockStart("Javadoc");
	return null;
    }

      /**
     * visits an array length expression
     */
    public Object visitCatchClause(JCatchClause self,
                                 JFormalParameter exception,
                                 JBlock body) 
    {
        blockStart("CatchClause", self);
        trash = exception.accept(this);
        trash = body.accept(this);
	return self;
    }

 
}
