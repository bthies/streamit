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
    /* The  one to one parent*/
    private SIRStream parentStream;
    /* The Top Level SIR Operator */
    private SIRStream topLevel;

    /*Object used to disregard the return values from the attributed
      visitor methods */
    private Object trash;

    private int num = 0;
    
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

    //globals to store the split and join type so they can be set in the
    //post visit of the class declaration
    private SIRSplitType splitType;
    private SIRJoinType joinType;



    public Kopi2SIR() {
	parentStream = null;
	topLevel = null;

	currentMethod = null;
	visitedSIROps = new Hashtable(100);
	symbolTable = new Hashtable(300);
	interfaceList = new Vector(100);
    }

    private void addVisitedOp(String className, SIROperator sirop) {
	if (visitedSIROps.get(className) != null)
	    at.dms.util.Utils.fail("Duplicate Definition of " + className);
	visitedSIROps.put(className, sirop);
    }
    private SIROperator getVisitedOp(String className) 
    {
	SIROperator ret = (SIROperator)visitedSIROps.get(className);
	return ret;
    }
    
    private void blockStart(String str) {
	//System.out.println("attribute_visit" + str +"\n");
    }

    private void printMe(String str) {
	//System.out.println(str);
    }

    
    /* creates a new SIROperator setting parentStream to this object
       also, if the SIROperator is one to one, parentStream is set */
    private SIROperator newSIROP(JClassDeclaration clazz) {
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
						    
	at.dms.util.Utils.fail("You are creating an unsupported streaMIT Operator: "
			       + TYPE + ".");
	return null;
    }
    
    /**
     * Returns true if the given JStatement defines a call to 
     * an SIR expression
     **/
    private boolean isSIRExp(JMethodCallExpression exp) {

	if (exp.getIdent().startsWith("push") ||
	    exp.getIdent().startsWith("pop")  ||
	    exp.getIdent().startsWith("peek") ||
	    exp.getIdent().startsWith("println") )
	    return true;
	
	return false;
    }

    /* Given a method call expression, this function will create/return a 
       SIR Expression if the method call orignally denoted one. */
    private JPhylum newSIRExp(JMethodCallExpression exp, JExpression[] args) {
	JPhylum newExp = null;
	printMe("In newSIRExp");
	if (args.length > 1)
	    at.dms.util.Utils.fail("Too many args to SIR call.");
	
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

    /* registers the SIR construct with its parent based on
       the type of the parentStream,  called only during a class declaration
       or anonymous creation
    */
    private void registerWithParent(SIRStream me) {
	if (me.getParent() == null)
	    return;
	if (me.getParent() instanceof SIRPipeline) {
	    if (!currentMethod.equals("add"))
		at.dms.util.Utils.fail("Anonymous Stream Creation not in add() in Pipeline");
	    ((SIRPipeline)me.getParent()).add(me);
	}
	else if (me.getParent() instanceof SIRFeedbackLoop) {
	    if (currentMethod.equals("setBody"))
		((SIRFeedbackLoop)me.getParent()).setBody(me);
	    else if (currentMethod.equals("setLoop"))
		((SIRFeedbackLoop)me.getParent()).setLoop(me);
	    else 
		at.dms.util.Utils.fail("Anonymous Stream Creation not in setBody() in FeedbackLoop");
	}
	else if (me.getParent() instanceof SIRSplitJoin) {
	    if (!currentMethod.equals("add"))
		at.dms.util.Utils.fail("Anonymous Stream Creation not in add() in SplitJoin");
	    ((SIRSplitJoin)me.getParent()).add(me);
	}
	else
	    at.dms.util.Utils.fail("Unimplemented SIRStream (cannot register)");
    }
    
   
    //Perform any post visiting operations after a class is declared
    private void postVisit(SIROperator current) {
	if (current instanceof SIRStream)
	    registerWithParent((SIRStream)current);
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

    private boolean supportedType(String type) {
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
	  

    private JMethodDeclaration[] buildPortalMethodArray(JClassDeclaration portal, JClassDeclaration clazz) 
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
		at.dms.util.Utils.fail("Cannot find a method declaration for portal method " + 
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
	
	blockStart("ClassDeclaration");
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
		at.dms.util.Utils.fail("Toplevel Stream already set");	
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
	
	blockStart("CompilationUnit");

        /*if (packageName.getName().length() > 0)
	  packageName.accept(this);*/

        /*for (int i = 0; i < importedPackages.length ; i++)
	  importedPackages[i].accept(this);*/

	/*  for (int i = 0; i < importedClasses.length ; i++)
	    importedClasses[i].accept(this);*/

	

        for (int i = 0; i < typeDeclarations.length ; i++)
	    trash = typeDeclarations[i].accept(this);

	

	/* the end of the visit */
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
	
	
	/* Install Empty Init Method */
	if (parentStream.getInit() == null && !(parentStream instanceof SIRPipeline)) {
	    JStatement[] emptybody = new JStatement[0];
	    JBlock emptyblock = new JBlock(null, emptybody, null);
	    JMethodDeclaration emptyInit = new JMethodDeclaration(null,
								  at.dms.kjc.
								  Constants.ACC_PUBLIC,
								  CStdType.Void,
								  "init",
								  JFormalParameter.EMPTY,
								  CClassType.EMPTY,
								  emptyblock,
								  null, null);
	    parentStream.setInit(emptyInit);
	}
	

        for (int i = 0; i < methods.length ; i++)
            trash = methods[i].accept(this); 
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
    private CType getType(String type) {
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
	    at.dms.util.Utils.fail("Cannot declare fields static");
	
	if (ident.equals("input")) {
	    if (!(parentStream instanceof SIRFilter))
		at.dms.util.Utils.fail("Input declaration on non-Filter");
	    SIRFilter filter = (SIRFilter)parentStream;
	    Vector v = (Vector)expr.accept(this);
	    filter.setInputType(getType((String)v.elementAt(0)));
	    filter.setPop(((JIntLiteral)v.elementAt(1)).intValue());
	    //If a peek value is given, and it is greater than pops
	    //set the peek
	    if (v.size() > 2) {
		if (((JIntLiteral)v.elementAt(2)).intValue() < 
		    ((JIntLiteral)v.elementAt(1)).intValue())
		    at.dms.util.Utils.fail("Peeks less than Pops!");
		filter.setPeek(((JIntLiteral)v.elementAt(2)).intValue());
	    }
	    else  //Otherwise set the peeks to the number of pops
		filter.setPeek(((JIntLiteral)v.elementAt(1)).intValue());
	    return self;
	}
	
	/* Output declaration set the fields for the current
	   stream */ 
	else if (ident.equals("output")) {
	    if (!(parentStream instanceof SIRFilter))
		at.dms.util.Utils.fail("Output declaration on non-Filter");
	    SIRFilter filter = (SIRFilter)parentStream;
	    Vector v = (Vector)expr.accept(this);
	    int push = ((JIntLiteral)v.elementAt(1)).intValue();
	    filter.setPush(push);
	    filter.setOutputType(getType((String)v.elementAt(0)));
	    return self;
	}
	else {   /*Normal field declaration, add this field */
	    if (expr != null)
		expr = (JExpression)expr.accept(this);
	    parentStream.addField(new JFieldDeclaration(null, new JVariableDefinition(null,
										      modifiers,
										      type,
										      ident,
										      expr),
							null, null));
	}
	//never reached!
	return self;
    }


    private boolean ignoreMethodDeclaration(String ident) {
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
        printMe("MethodDeclaration: " + ident);
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
		at.dms.util.Utils.fail("Work Function Declared for Non-Filter");
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
		at.dms.util.Utils.fail("initPath declared for non-Feedbackloop");
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
        blockStart("ConstructorDeclaration");
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
        blockStart("WhileStatement");
        JExpression newCond = (JExpression)cond.accept(this);
        JStatement newBody = (JStatement)body.accept(this);
	return new JWhileStatement(null, newCond, newBody, null);
    }

    /**
   * visits a variable declaration statement
   */
    public Object visitVariableDeclarationStatement
        (JVariableDeclarationStatement self,
         JVariableDefinition[] vars)
    {
        blockStart("VariableDeclarationStatement");
        
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
        blockStart("VariableDefinition: " + self.getIdent());
	printMe(type.toString());

	if (expr != null) 
	    expr = (JExpression)expr.accept(this);

	//we are defining a new variable with a "new" in the code
 	if (expr instanceof JUnqualifiedInstanceCreation) {
	    if (type.toString().endsWith("Portal")) {
		//Portal Definition , SIRCreatePortal add to symbol Table
		//make the SIRCreatePortal the new expression in the definition
		SIRCreatePortal cp = new SIRCreatePortal();
		return new JVariableDefinition(null, modifiers, type, ident, cp);
	    }
	    //If the class of this variable is in the SIR table then
	    //we need to add this definition to the symbol table
	    else if (getVisitedOp(type.toString()) != null) {
		SIRStream ST = (SIRStream)ObjectDeepCloner.deepCopy(getVisitedOp(type.toString()));
		printMe("Adding " + ident + " to symbol table");
		
		SIRInitStatement newSIRInit = new SIRInitStatement(null, 
								   null, 
								   ((JUnqualifiedInstanceCreation)expr).
								   getParams(), 
								   ST);
		symbolTable.put(self, newSIRInit);
		return null;
		//return new JVariableDefinition(null, modifiers, type, ident, expr);
	    }
	    else 
		at.dms.util.Utils.fail("Definition of Stream " + type.toString() + 
				       " not seen before definition of " + ident);
	}
	return new JVariableDefinition(null, modifiers, type, ident, expr);
    }
    
    /**
     * visits a switch statement
     */
    public Object visitSwitchStatement(JSwitchStatement self,
                                     JExpression expr,
                                     JSwitchGroup[] body)
    {
        blockStart("SwitchStatement");
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
        blockStart("Return");
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
        blockStart("LabeledStatement");
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
        blockStart("IfStatement");
        cond = (JExpression)cond.accept(this);
        thenClause= (JStatement)thenClause.accept(this);
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
        blockStart("ForStatement");
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
        blockStart("CompoundStatement");
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
        blockStart("ExpressionStatement");
	
	JPhylum attrib = (JPhylum)expr.accept(this);
	

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
        blockStart("ExpressionListStatement");
        for (int i = 0; i < expr.length; i++)
            expr[i] = (JExpression)expr[i].accept(this);
        return new JExpressionListStatement(null, expr, null);
    }

    /**
   * visits a empty statement
   */
    public Object visitEmptyStatement(JEmptyStatement self)
    {
        blockStart("EmptyStatement");
        return self;
    }

    /**
   * visits a do statement
   */
    public Object visitDoStatement(JDoStatement self,
                                 JExpression cond,
                                 JStatement body)
    {
        blockStart("DoStatement");
	JExpression newCond = (JExpression)cond.accept(this);
	JStatement newBody = (JStatement)body.accept(this);
	return new JDoStatement(null, newCond, newBody, null);
    }

    /**
     * visits a continue statement
     */
    public Object visitContinueStatement(JContinueStatement self,
                                       String label)
    {
        blockStart("ContinueStatement");	
	return self;
    }

    /**
     * visits a break statement
     */
    public Object visitBreakStatement(JBreakStatement self,
                                    String label)
    {
        blockStart("BreakStatement");
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
	
	blockStart("BlockStatement");
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
        blockStart("TypeDeclarationStatement");
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
        blockStart("UnaryPlusExpression");
        JExpression newExpr = (JExpression)expr.accept(this);   
	return new JUnaryPlusExpression(null, newExpr);
    }

    /**
     * visits an unary minus expression
     */
    public Object visitUnaryMinusExpression(JUnaryExpression self,
                                          JExpression expr)
    {
        blockStart("UnaryMinusExpression");
	JExpression newExpr = (JExpression)expr.accept(this);   
	return new JUnaryMinusExpression(null, newExpr);
    }

    /**
     * visits a bitwise complement expression
     */
    public Object visitBitwiseComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        blockStart("BitwiseComplementExpression");
	JExpression newExpr = (JExpression)expr.accept(this);   
	return new JBitwiseComplementExpression(null, newExpr);
    }

    /**
     * visits a logical complement expression
     */
    public Object visitLogicalComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        blockStart("LogicalComplementExpression");
	JExpression newExpr = (JExpression)expr.accept(this);   
	return new JLogicalComplementExpression(null, newExpr);
    }

    /**
     * visits a type name expression
     */
    public Object visitTypeNameExpression(JTypeNameExpression self,
                                        CType type)
    {
        blockStart("TypeNameExpression");
	return self;
    }

    /**
     * visits a this expression
     */
    public Object visitThisExpression(JThisExpression self,
                                    JExpression prefix)
    {
        blockStart("ThisExpression");
	if (prefix != null)
	    prefix = (JExpression)prefix.accept(this);
	return new JThisExpression(null, prefix);
    }

    /**
     * visits a super expression
     */
    public Object visitSuperExpression(JSuperExpression self)
    {
        blockStart("SuperExpression");
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
        blockStart("ShiftExpression");
	JExpression newLeft = (JExpression)left.accept(this);
	JExpression newRight = (JExpression)right.accept(this);
	return new JShiftExpression(null, oper, newLeft, newRight);
    }

    /**
     * visits a shift expressiona
     */
    public Object visitRelationalExpression(JRelationalExpression self,
                                          int oper,
                                          JExpression left,
                                          JExpression right)
    {
        blockStart("RelationalExpression");
	JExpression newLeft = (JExpression)left.accept(this);
	JExpression newRight = (JExpression)right.accept(this);
	return new JRelationalExpression(null, oper, newLeft, newRight);
    }

    /**
     * visits a prefix expression
     */
    public Object visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr)
    {
        blockStart("PrefixExpression");
        expr = (JExpression)expr.accept(this);
	return new JPrefixExpression(null, oper, expr);
    }

    /**
     * visits a postfix expression
     */
    public Object visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr)
    {
        blockStart("PostfixExpression");
	expr = (JExpression)expr.accept(this);
	return new JPostfixExpression(null, oper, expr);
    }

    /**
     * visits a parenthesed expression
     */
    public Object visitParenthesedExpression(JParenthesedExpression self,
                                           JExpression expr)
    {
        // Not parenthesized?
        blockStart("ParenthesedExpression");
	JExpression newExpr = (JExpression)expr.accept(this);   
	return new JParenthesedExpression(null, newExpr);
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
        blockStart("QualifiedAnonymousCreation");
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
        blockStart("QualifiedInstanceCreation");
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
        blockStart("UnqualifiedAnonymousCreation");
	//Get the SIR Operator back from the Class Declaration
	SIROperator SIROp = (SIROperator)decl.accept(this);
	if (!(SIROp instanceof SIRStream))
	    at.dms.util.Utils.fail("Trying to anonymously create non-Stream");
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
        blockStart("UnqualifiedInstanceCreation");
	printMe("  Declaring: " + type.getIdent());
	if (params.length == 0)
	    params = JExpression.EMPTY;
	if (type.getIdent().equals("Channel")) 
	    /*Channel declaration, treat the args as special */
	{
	    String channelType = (String)params[0].accept(this);
	    JIntLiteral num = (JIntLiteral)params[1].accept(this);
	    Vector v = new Vector(3);
	    v.add(channelType);
	    v.add(num);
	    JIntLiteral num2;
	    if (params.length > 2) { 
		num2 = (JIntLiteral)params[2].accept(this);
		v.add(num2);
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
        blockStart("NewArrayExpression");
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
        blockStart("NameExpression");
	if (prefix != null)
	    prefix = (JExpression)prefix.accept(this);
	return new JNameExpression(null, prefix, ident);
    }

    /**
     * visits an array allocator expression
     */
    public Object visitBinaryExpression(JBinaryExpression self,
                                      String oper,
                                      JExpression left,
                                      JExpression right)
    {
        blockStart("BinaryExpression");
	JExpression newLeft = (JExpression)left.accept(this);
        JExpression newRight = (JExpression)right.accept(this);
	if (oper.equals("+"))
	    return new JAddExpression(null, newLeft, newRight);
	else if (oper.equals("||"))
	    return new JConditionalOrExpression(null, newLeft, newRight);
	else if (oper.equals("/"))
	    return new JDivideExpression(null, newLeft, newRight);
	else if (oper.equals("-"))
	    return new JMinusExpression(null, newLeft, newRight);
	else if (oper.equals("%"))
	    return new JModuloExpression(null, newLeft, newRight);
	else if (oper.equals("*"))
	    return new JMultExpression(null, newLeft, newRight);
	else
	    at.dms.util.Utils.fail("Unknown binary expression");
	return new JDivideExpression(null, newLeft, newRight);
    }


    private SIRMessageStatement createMessageStatement(JMethodCallExpression methCall,
						       CType portal, 
						       JExpression prefix, 
						       JExpression[] args) {
	//index of the method in the portal interface
	int index = -1;
	//Process the args
	for (int i = 0; i < args.length; i++)
	    args[i] = (JExpression) args[i].accept(this);

	//Find the index of the method call
	CClassType[] interfaces = 
	    portal.getCClass().getInterfaces();
	
	if (interfaces.length > 1)
	    at.dms.util.Utils.fail("A portal can only implement one interface at this time");
	
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
						      SIRStream st) {
	CMethod meth = methCall.getMethod();
	//the index of the method in the interface
	
	//Extract the interface from the portal prefix variable
	CClassType[] interfaces = 
	    portal.getCClass().getInterfaces();
	
	if (interfaces.length > 1)
	    at.dms.util.Utils.fail("A portal can only implement one interface at this time");
	
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
		at.dms.util.Utils.fail("Cannot find portal method in portal interface (RegReceiver)");
	    matchedMethods[i] = currentMethod;
	}
	

	return new SIRRegReceiverStatement(prefix,st, matchedMethods);
    }


	    	/*
	//Find the method index 
	for (int i = 0; i < methods.length; i++)
	    if (methods[i].equals(meth)) {
		index = i;
		break;
	    }
	if (index == -1)
	    at.dms.util.Utils.fail("Cannot find portal method in portal interface");
	*/

						      


    //This method creates an initStatement from the underlying arguments
    //to the function should be translated into an init statment
    //also, based on the String argument, it registers the SIROp
    //with the Stream
    private SIRInitStatement createInitStatement(Object SIROp, String regMethod) 
    {
	SIRStream newST = null;

	//Already an SIRinit statement
	if (SIROp instanceof SIRInitStatement) {
	    return (SIRInitStatement)SIROp;
	}
	//Using a local variable that has been already defined as a stream construct
	if (SIROp instanceof SIRStream)
	    newST = (SIRStream)SIROp;

	/********************************************************************/
	/* THIS BREAKS IT, but was inserted in place of what's above (up to */
	/* "= null"                                                         */
	/********************************************************************
	//Already an SIRinit statement (this is a local variable expression)
	//we used the init statement from the symbol table
	//Using a local variable that has been already defined as a stream construct
	if (SIROp instanceof SIRInitStatement) {
	    //now we must set the parent and do any other steps that are necessary 
	    // when registering
	    newST = (SIRStream)((SIRInitStatement)SIROp).getTarget();
	    newST.setParent((SIRContainer)parentStream);
	}
	if (SIROp instanceof SIRStream)
	    newST = (SIRStream)SIROp;
	/********************************************************************/
	/************** END BREAKAGE ****************************************/
	/********************************************************************/


	//Creating a named class
	//if it is a named creation, lookup in the symbol table for the visited
	//node and add it to the pipeline
	if (SIROp instanceof JUnqualifiedInstanceCreation) {
	    SIRStream st = (SIRStream)getVisitedOp(((JUnqualifiedInstanceCreation)SIROp).
						   getType().getCClass().getIdent());
	    
	    newST = (SIRStream) ObjectDeepCloner.deepCopy(st);
	    //SIRStream newST = (SIRStream) st.clone();
	    newST.setParent((SIRContainer)parentStream);
	}
	//Die if it is none of the above case
	if (newST == null)
	    at.dms.util.Utils.fail("Illegal Arg to Stream Add Construct");
	
	//now find the registration method
	if (regMethod.equals("add")) {
	    if (parentStream instanceof SIRPipeline)
		((SIRPipeline)parentStream).add(newST);
	    else 
		((SIRSplitJoin)parentStream).add(newST);
	}
	else if (regMethod.equals("setBody"))
	    ((SIRFeedbackLoop)parentStream).setBody(newST);
	else if (regMethod.equals("setLoop"))
	    ((SIRFeedbackLoop)parentStream).setLoop(newST);
	else
	    at.dms.util.Utils.fail("Invalid Registration Method");

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
	blockStart("MethodCallExpression: " + self.getIdent());
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
		at.dms.util.Utils.fail("Exactly one arg to add() allowed");
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
		at.dms.util.Utils.fail("regReceiver all must have a portal prefix");
	    return null;
	}
	else if (ident.equals("add")) {            //Handle an add call in a pipeline
	    //Parent must be a pipeline
	    if (!((parentStream instanceof SIRPipeline) || parentStream instanceof SIRSplitJoin)) 
		at.dms.util.Utils.fail("Add not called on Pipeline or SplitJoin");
	    if (args.length > 1)
		at.dms.util.Utils.fail("Exactly one arg to add() allowed");
	    //Visit the argument (Exactly one))
	    Object SIROp = args[0].accept(this);
	    	    
	    //reset currentMethod on all returns
	    currentMethod = parentMethod;
	    //create the init statement to return
	    return createInitStatement(SIROp, ident);
	    
	} else if (ident.equals("setDelay")) {
	    if (!(parentStream instanceof SIRFeedbackLoop))
		at.dms.util.Utils.fail("SetDelay called on Non-FeedbackLoop");
	    if (args.length > 1)
		at.dms.util.Utils.fail("Too many args to setDelay");
	    int delay  = ((JIntLiteral)args[0].accept(this)).intValue();
	    ((SIRFeedbackLoop)parentStream).setDelay(delay);
	    //reset currentMethod on all returns
	    currentMethod = parentMethod;
	    //we want to ignore remove this method from the block
	    return null;
	}
	else if (ident.equals("setBody")) {
	    if (!(parentStream instanceof SIRFeedbackLoop)) 
		at.dms.util.Utils.fail("setBody called on non-FeedbackLoop");
	    if (args.length > 1)
		at.dms.util.Utils.fail("Exactly one arg to setBody() allowed");
	    //Visited the argument
	    Object SIROp = args[0].accept(this);
	    //reset currentMethod on all returns
	    currentMethod = parentMethod;
	    //create the init statement to return
	    return createInitStatement(SIROp, ident);
	}
	else if (ident.equals("setLoop")) {
	    if (!(parentStream instanceof SIRFeedbackLoop)) 
		at.dms.util.Utils.fail("setLoop called on non-FeedbackLoop");
	    if (args.length > 1)
		at.dms.util.Utils.fail("Exactly one arg to setLoop() allowed");
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
	}
	//reset currentMethod
	currentMethod = parentMethod;
	return new JMethodCallExpression(null, prefix, ident, args);
    }
    

    private void buildSplitter(JExpression type) 
    {
	if (!(type instanceof JMethodCallExpression))
	    at.dms.util.Utils.fail("arg to setSplitter must be a method call");

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
	    at.dms.util.Utils.fail("Unsupported Split Type");
	
    }

    private void buildJoiner(JExpression type) 
    {
	if (!(type instanceof JMethodCallExpression))
	    at.dms.util.Utils.fail("arg to setJoiner must be a method call");

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
        blockStart("LocalVariableExpression");
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
        blockStart("EqualityExpression");
	if (left != null)
	    left = (JExpression)left.accept(this);
	if (right != null)
	    right = (JExpression)right.accept(this);
	return new JEqualityExpression(null, equal, left, right);
    }

    /**
     * visits a conditional expression
     */
    public Object visitConditionalExpression(JConditionalExpression self,
                                           JExpression cond,
                                           JExpression left,
                                           JExpression right)
    {
        blockStart("ConditionalExpression");
	if (cond != null)
	    cond = (JExpression)cond.accept(this);
	if (left != null)
	    left = (JExpression)left.accept(this);
	if (right != null)
	    right = (JExpression)right.accept(this);
	
	return new JConditionalExpression(null, cond, left, right);
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
        blockStart("CompoundAssignmentExpression");
	if (left != null)
	    left = (JExpression)left.accept(this);
	if (right != null)
	    right = (JExpression)right.accept(this);
	return new JCompoundAssignmentExpression(null, oper, left, right);
    }

    /**
     * visits a field expression
     */
    public Object visitFieldExpression(JFieldAccessExpression self,
                                     JExpression left,
                                     String ident)
    {
        blockStart("FieldExpression");
	if (supportedType(left.getType().getCClass().getIdent()))
	    return left.getType().getCClass().getIdent();
	else 
	    left = (JExpression)left.accept(this);
	return new JFieldAccessExpression(null, left, ident);
    }

    /**
   * visits a class expression
   */
    public Object visitClassExpression(JClassExpression self,
                                     CType type)
    {
        blockStart("ClassExpression");
	return self;
    }

    /**
     * visits a cast expression
     */
    public Object visitCastExpression(JCastExpression self,
                                    JExpression expr,
                                    CType type)
    {
        blockStart("CastExpression");
	expr = (JExpression)expr.accept(this);
	return new JCastExpression(null, expr, type);
    }

    /**
     * visits a cast expression
     */
    public Object visitUnaryPromoteExpression(JUnaryPromote self,
                                            JExpression expr,
                                            CType type)
    {
        blockStart("UnaryPromoteExpression");
	expr = (JExpression)expr.accept(this);
	return new JUnaryPromote(expr, type);
    }

    /**
     * visits a compound assignment expression
     */
    public Object visitBitwiseExpression(JBitwiseExpression self,
                                       int oper,
                                       JExpression left,
                                       JExpression right)
    {
        blockStart("BitwiseExpression");
	if (left != null)
	    left = (JExpression)left.accept(this);
	if (right != null)
	    right = (JExpression)right.accept(this);
	return new JBitwiseExpression(null, oper, left, right);
    }
    
    /**
     * visits an assignment expression
     */
    public Object visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right)
    {
        blockStart("AssignmentExpression");
       	if (left != null)
	    left = (JExpression)left.accept(this);
	if (right != null)
	    right = (JExpression)right.accept(this);
	return new JAssignmentExpression(null, left, right);
    }

    /**
     * visits an array length expression
     */
    public Object visitArrayLengthExpression(JArrayLengthExpression self,
                                           JExpression prefix)
    {
        blockStart("ArrayLengthExpression");
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
        blockStart("ArrayAccessExpression");
	if (prefix != null)
	    prefix = (JExpression)prefix.accept(this);
	if (accessor != null)
	    accessor = (JExpression)accessor.accept(this);
	return new JArrayAccessExpression(null, prefix, accessor);
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
        blockStart("SwitchLabel");
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
        blockStart("SwitchGroup");
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
        blockStart("FormalParameter");
	return self;
    }

    /**
     * visits an array length expression
     */
    public Object visitConstructorCall(JConstructorCall self,
                                     boolean functorIsThis,
                                     JExpression[] params)
    {
        blockStart("ConstructorCall");
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
        blockStart("ArrayInitializer");
	if (elems.length == 0)
	    elems = JExpression.EMPTY;
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
        blockStart("BooleanLiteral");
	return new JBooleanLiteral(null, value);
    }

    /**
     * visits a byte literal
     */
  public Object visitByteLiteral(JByteLiteral self,
			  byte value)
    {
        blockStart("ByteLiteral");
	return new JByteLiteral(null, value);
    }

    /**
     * visits a character literal
     */
  public Object visitCharLiteral(JCharLiteral self,
			  char value)
    {
        blockStart("CharLiteral");
	return new JCharLiteral(null, value);
    }

    /**
     * visits a double literal
     */
  public Object visitDoubleLiteral(JDoubleLiteral self,
			    double value)
    {
        blockStart("DoubleLiteral");
	return new JDoubleLiteral(null, value);
    }

    /**
     * visits a float literal
     */
  public Object visitFloatLiteral(JFloatLiteral self,
			   float value)
    {
        blockStart("FloatLiteral");
	return new JFloatLiteral(null, value);
    }

    /**
     * visits a int literal
     */
  public Object visitIntLiteral(JIntLiteral self,
			 int value)
    {
        blockStart("IntLiteral");
	return new JIntLiteral(null,value);
    }

    /**
     * visits a long literal
     */
  public Object visitLongLiteral(JLongLiteral self,
			  long value)
    {
        blockStart("LongLiteral");
	return new JLongLiteral(null, value);
    }

    /**
     * visits a short literal
     */
  public Object visitShortLiteral(JShortLiteral self,
			   short value)
    {
        blockStart("ShortLiteral");
	return new JShortLiteral(null, value);
    }

    /**
     * visits a string literal
     */
  public Object visitStringLiteral(JStringLiteral self,
			    String value)
    {
        blockStart("StringLiteral");
	return new JStringLiteral(null, value);
    }

    /**
     * visits a null literal
     */
  public Object visitNullLiteral(JNullLiteral self)
    {
        blockStart("NullLiteral");
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
    public JInterfaceDeclaration[] getInterfaces() {
	JInterfaceDeclaration[] ret = (JInterfaceDeclaration[])interfaceList.toArray(new JInterfaceDeclaration[0]);
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
	blockStart("visitInterfaceDeclaration");
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
        blockStart("TryCatchStatement");
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
        blockStart("TryFinallyStatement");
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
        blockStart("ThrowStatement");
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
        blockStart("SynchronizedStatement");
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
        blockStart("InstanceOfExpression");
	trash = expr.accept(this);
	return self;
    }

      /**
     * visits an array length expression
     */
    public Object visitComments(JavaStyleComment[] comments) {
	blockStart("Comments");
	return comments;
    }

    /**
     * visits an array length expression
     */
    public Object visitComment(JavaStyleComment comment) {
	blockStart("Comment");
	return comment;
    }

    /**
     * visits an array length expression
     */
    public Object visitJavadoc(JavadocComment comment) {
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
        blockStart("CatchClause");
        trash = exception.accept(this);
        trash = body.accept(this);
	return self;
    }

 
}
