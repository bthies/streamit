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
import java.util.Vector;
import java.util.Hashtable;

public class Kopi2SIR extends Utils implements AttributeVisitor
{
    /* The  one to one parent*/
    private SIRStream parentStream;
    /* the SIROperator parent (may equal parentStream) */
    private SIROperator parentOperator;
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


    public Kopi2SIR() {
	parentStream = null;
	topLevel = null;
	parentOperator = null;
	currentMethod = null;
	visitedSIROps = new Hashtable(100);
    }

    private void addVisitedOp(String className, SIROperator sirop) {
	visitedSIROps.put(className, sirop);
    }
    private SIROperator getVisitedOp(String className) 
    {
	SIROperator ret = (SIROperator)visitedSIROps.get(className);
	if (ret == null)
	    at.dms.util.Utils.fail("Named SIR Operator not found in the symbol table");
	return ret;
    }
    
    private void blockStart(String str) {
	//System.out.println("attribute_visit" + str +"\n");
    }

    private void printMe(String str) {
	//System.out.println(str);
    }

    
    /* creates a new SIROperator setting parentOperator to this object
       also, if the SIROperator is one to one, parentStream is set */
    private SIROperator newSIROP(JClassDeclaration clazz) {
	String TYPE = clazz.getSourceClass().getSuperClass().getIdent();
    
	if (TYPE.equals("Pipeline")) {
	    SIRPipeline current = new SIRPipeline(parentStream, 
				   JFieldDeclaration.EMPTY,
				   JMethodDeclaration.EMPTY);
	    parentOperator = parentStream = current;
	    return current;
	}
	if (TYPE.equals("Filter")) {
	    SIRFilter current = new SIRFilter();
	    current.setParent(parentStream);
	    current.setFields(JFieldDeclaration.EMPTY);
	    current.setMethods(JMethodDeclaration.EMPTY);
	    parentOperator = parentStream = current;
	    return current;
	}
	if (TYPE.equals("FeedbackLoop")) {
	    SIRFeedbackLoop current = new SIRFeedbackLoop();
	    current.setParent(parentStream);
	    current.setFields(JFieldDeclaration.EMPTY);
	    current.setMethods(JMethodDeclaration.EMPTY);
	    parentOperator = parentStream = current;
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


    /* given a string of the form push*, pop*, or peek*.  This method
       returns the CType of corresponding to the *. */
    private CType getTapeType(String exp) {
	// based on the string type, set the type
	
	String type = "";

	// Extract type
	if (exp.startsWith("push") || exp.startsWith("peek"))
	    type = exp.substring(4);
	else if(exp.startsWith("pop"))
	    type = exp.substring(3);
	else
	    at.dms.util.Utils.fail("SIR Expression Expected");
	//Return CType based on extracted Type String
	if (type.equals("Int"))
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
	    at.dms.util.Utils.fail("Non-Supported Type for SIR Push/Pop/Peek");
	return null;
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
	    ((SIRPushExpression)newExp).setTapeType(getTapeType(exp.getIdent()));
	}
	if (exp.getIdent().startsWith("pop")) {
	    newExp = new SIRPopExpression();
	    ((SIRPopExpression)newExp).setTapeType(getTapeType(exp.getIdent()));
	}
	if (exp.getIdent().startsWith("peek")) {
	    newExp = new SIRPeekExpression(args[0]);
	    ((SIRPeekExpression)newExp).setTapeType(getTapeType(exp.getIdent())); 
	}
	if (exp.getIdent().startsWith("println")) {
	    newExp = new SIRPrintStatement(null, args[0], null);
	}
	
	return newExp;
    }

    /* registers the SIR construct with its parent based on
       the type of the parentStream */
    private void registerWithParent(SIRStream me) {
	if (me.getParent() == null)
	    return;
	if (me.getParent() instanceof SIRPipeline) {
	    if (!currentMethod.equals("add"))
		at.dms.util.Utils.fail("Anonymous Stream Creation not in add() in Pipeline");
	    ((SIRPipeline)me.getParent()).add(me);
	}
	else if (me.getParent() instanceof SIRFeedbackLoop) {
	    //Can only add using setBody in Feedbackloop
	    if (currentMethod.equals("setBody"))
		((SIRFeedbackLoop)me.getParent()).setBody(me);
	    else if (currentMethod.equals("setLoop"))
		((SIRFeedbackLoop)me.getParent()).setLoop(me);
	    else 
		at.dms.util.Utils.fail("Anonymous Stream Creation not in setBody() in FeedbackLoop");
	}
	else
	    at.dms.util.Utils.fail("Unimplemented SIRStream (cannot register)");
    }
    
    private void buildInit(SIRPipeline node) {
	JStatement[] initStatements = new JStatement[node.size()];
	//Add to the init statement any anonymously created SIR ops
	//that are already registered
	for (int i = 0; i < node.size(); i++) {
	    initStatements[i] = new SIRInitStatement(null, null, JExpression.EMPTY,
						     node.get(i));
	}
	node.setInit(new JMethodDeclaration(null,
					    at.dms.kjc.Constants.ACC_PUBLIC,
					    CStdType.Void,
					    "init",
					    JFormalParameter.EMPTY,
					    CClassType.EMPTY,
					    new JBlock(null, initStatements, null),
					    null,
					    null));
    }

    //Perform any post visiting operations after a class is declared
    private void postVisit(SIROperator current) {
	if (current instanceof SIRStream)
	    registerWithParent((SIRStream)current);
	if (current instanceof SIRPipeline) 
	    buildInit((SIRPipeline) current);
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
	  



    public Object visitClassDeclaration(JClassDeclaration self,
                                      int modifiers,
                                      String ident,
                                      String superName,
                                      CClassType[] interfaces,
                                      JPhylum[] body,
                                      JMethodDeclaration[] methods,
                                      JTypeDeclaration[] decls)
    {
	/* The current SIR Operator we are creating */
	SIROperator current;
	SIROperator oldParentOperator = parentOperator;
	SIRStream oldParentStream = parentStream;
	/* true if this class was created with an anonymous creation */
	boolean anonCreation = false;
	
	blockStart("ClassDeclaration");
	printMe("Class Name: " + ident);
	if (self.getSourceClass() != null)
	    printMe("In " + 
		    self.getSourceClass().getSuperClass().getIdent());

	// if the name of the class being declared is the same
	// as the name of the superclass then it is anonymous
	if (ident == self.getSourceClass().getSuperClass().getIdent())
	    anonCreation = true;
	
	// create a new SIROperator
	current = newSIROP(self);

	//If this class is public then it is the topLevel stream (entry point)
	if (self.getSourceClass().isPublic()) {
	    if (topLevel == null)
		topLevel = (SIRStream) current;
	    else
		at.dms.util.Utils.fail("Toplevel Stream already set");	
	}
	if (current == null) 
	    printMe("Null");
	
	trash = visitClassBody(decls, methods, body);

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
	parentOperator = oldParentOperator;
	
	printMe( "Out " + self.getSourceClass().getSuperClass().getIdent()
			    + num);
	return self;
    }

    public Object visitCompilationUnit(JCompilationUnit self,
                                     JPackageName packageName,
                                     JPackageImport[] importedPackages,
                                     JClassImport[] importedClasses,
                                     JTypeDeclaration[] typeDeclarations)
    {
	/* The beginnin of the visit! */
	
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
                               JMethodDeclaration[] methods,
                               JPhylum[] body)
    {
	blockStart("ClassBody");
	for (int i = 0; i < decls.length ; i++)
            trash = decls[i].accept(this);
	
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
        for (int i = 0; i < body.length ; i++)
            trash = body[i].accept(this);
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
                                           JMethodDeclaration[] methods)
    {
	printMe("visitInnerClassDeclaration");
        trash = visitClassBody(decls, methods, body);
        return self;
    }
    

    /* this methods sets the input type of the given filter.  If
       Type gives the type */
    private void setInputType(SIRFilter filter, String type) {
	// based on the string type, set the type
	if (type.equals("Integer"))
	    filter.setInputType(CStdType.Integer);
	else if (type.equals("Character"))
	    filter.setInputType(CStdType.Char);
	else if (type.equals("Boolean"))
	    filter.setInputType(CStdType.Boolean);
	else if (type.equals("Byte"))
	    filter.setInputType(CStdType.Byte);
	else if (type.equals("Double"))
	    filter.setInputType(CStdType.Double);
	else if (type.equals("Float"))
	    filter.setInputType(CStdType.Float);
	else if (type.equals("Short"))
	    filter.setInputType(CStdType.Short);
	else if (type.equals("String"))
	    filter.setInputType(CStdType.String);	
	else if (type.equals("Long"))
	    filter.setInputType(CStdType.Long);
	else
	    at.dms.util.Utils.fail("Non-Supported Type for Filter Input");
    }


    /* this methods sets the output type of the given filter.  If
       Type gives the type */
    private void setOutputType(SIRFilter filter, String type) {
	// based on the string type, set the type
	if (type.equals("Integer"))
	    filter.setOutputType(CStdType.Integer);
	else if (type.equals("Character"))
	    filter.setOutputType(CStdType.Char);
	else if (type.equals("Boolean"))
	    filter.setOutputType(CStdType.Boolean);
	else if (type.equals("Byte"))
	    filter.setOutputType(CStdType.Byte);
	else if (type.equals("Double"))
	    filter.setOutputType(CStdType.Double);
	else if (type.equals("Float"))
	    filter.setOutputType(CStdType.Float);
	else if (type.equals("Short"))
	    filter.setOutputType(CStdType.Short);
	else if (type.equals("String"))
	    filter.setOutputType(CStdType.String);	
	else if (type.equals("Long"))
	    filter.setOutputType(CStdType.Long);
	else
	    at.dms.util.Utils.fail("Non-Supported Type for Filter Output");
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
	if (ident.equals("input")) {
	    if (!(parentStream instanceof SIRFilter))
		at.dms.util.Utils.fail("Input declaration on non-Filter");
	    SIRFilter filter = (SIRFilter)parentStream;
	    Vector v = (Vector)expr.accept(this);
	    setInputType(filter, (String)v.elementAt(0));
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
	    setOutputType(filter, (String)v.elementAt(0));
	    return self;
	}
	else {   /*Normal field declaration, add this field */
	    parentStream.addField(self);
	}

	if (expr != null) 
	    trash = expr.accept(this);
	return self;
    }


    private boolean ignoreMethodDeclaration(String ident) {
	if (ident.equals("init") ||
	    ident.equals("work") ||
	    ident.equals("add") ||
	    ident.equals("initIO") ||
	    ident.equals("initPath"))
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
	if (parameters.length == 0)
	    parameters = JFormalParameter.EMPTY;
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
	else if (ident.equals("init") && (parentStream instanceof SIRFilter)) {
	    ((SIRFilter)parentStream).setInit(new JMethodDeclaration(null,
								     modifiers,
								     returnType,
								     ident,
								     parameters,
								     exceptions,
								     body,
								     null,
								     null)); 
	}
	else if (ident.equals("initPath")) {
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
        trash = cond.accept(this);
        trash = body.accept(this);
	return self;
    }

    /**
   * visits a variable declaration statement
   */
    public Object visitVariableDeclarationStatement
        (JVariableDeclarationStatement self,
         JVariableDefinition[] vars)
    {
        blockStart("VariableDeclarationStatement");
        
	for (int i = 0; i < vars.length; i++)
            trash = vars[i].accept(this);	
	return self;
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
	if (expr != null) 
	    trash = expr.accept(this);
        return self;
    }
    
    /**
     * visits a switch statement
     */
    public Object visitSwitchStatement(JSwitchStatement self,
                                     JExpression expr,
                                     JSwitchGroup[] body)
    {
        blockStart("SwitchStatement");
        trash = expr.accept(this);
        for (int i = 0; i < body.length; i++)
            trash = body[i].accept(this);
        return self;
    }

    /**
     * visits a return statement
     */
    public Object visitReturnStatement(JReturnStatement self,
                                     JExpression expr)
    {
        blockStart("Return");
        if (expr != null) 
	    trash = expr.accept(this);
        return self;
    }

    /**
     * visits a labeled statement
     */
    public Object visitLabeledStatement(JLabeledStatement self,
                                      String label,
                                      JStatement stmt)
    {
        blockStart("LabeledStatement");
	trash = stmt.accept(this);
        return self;
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
        trash = cond.accept(this);
        trash = thenClause.accept(this);
        if (elseClause != null) 
	    trash = elseClause.accept(this);
        return self;
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
	trash = body.accept(this);
	return self;
    }

    /**
   * visits a compound statement
   */
    public Object visitCompoundStatement(JCompoundStatement self,
                                       JStatement[] body)
    {
        blockStart("CompoundStatement");
        for (int i = 0; i < body.length; i++)
            trash = body[i].accept(this);
        return self;
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
            trash = expr[i].accept(this);
	if (expr.length == 0)
	    expr = JExpression.EMPTY;
        return self;
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
	trash = body.accept(this);
	return self;
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
                                    JStatement[] body,
                                    JavaStyleComment[] comments)
    {
       
	blockStart("BlockStatement");
        for (int i = 0; i < body.length; i++) {
	    Object st = body[i].accept(this);
	    if (st instanceof JStatement)
		body[i] = (JStatement)st;
	}
	return (new JBlock(null, body, null));
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
	return self;
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
        trash = expr.accept(this);
	return self;
    }

    /**
     * visits a postfix expression
     */
    public Object visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr)
    {
        blockStart("PostfixExpression");
	trash = expr.accept(this);
	return self;
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
	for (int i = 0; i < params.length; i++)
            trash = params[i].accept(this);
	if (params.length == 0)
	    params = JExpression.EMPTY;
	for (int i = 0; i < decl.fields.length; i++) {
	    printMe("   var: " + decl.fields[i].variable.getValue().getIdent());
	    trash = decl.fields[i].variable.accept(this); 
	}
	return self;
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
	for (int i = 0; i < params.length; i++)
            trash = params[i].accept(this);
	if (params.length == 0)
	    trash = params = JExpression.EMPTY;
	return self;
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
	decl.accept(this);
	for (int i = 0; i < params.length; i++)
            trash = params[i].accept(this);
	if (params.length == 0)
	    trash = params = JExpression.EMPTY;
	      /*	
			for (int i = 0; i < decl.fields.length; i++) {
			trash = decl.fields[i].variable.accept(this); 
			}
	      */
	return self;
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
	else {     /* Not a channel, trash args */
	    for (int i = 0; i < params.length; i++) 
		trash = params[i].accept(this);
	}
	return self;
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
            trash = dims[i].accept(this);
	return self;
    }

    /**
     * visits a name expression
     */
    public Object visitNameExpression(JNameExpression self,
                                    JExpression prefix,
                                    String ident)
    {
        blockStart("NameExpression");
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
	else if (ident.equals("add")) {            //Handle an add call in a pipeline
	    //Parent must be a pipeline
	    if (!(parentStream instanceof SIRPipeline)) 
		at.dms.util.Utils.fail("Add method called on Non-Pipeline");
	    if (args.length > 1)
		at.dms.util.Utils.fail("Exactly one arg to add() allowed");
	    //Visit the argument (Exactly one)
	    JExpression SIROp = (JExpression) args[0].accept(this);
	    //if it is a anonymous creation do nothing, it will be added in visitClassDecl
	    //if it is a named creation, lookup in the symbol table for the visited
	    //node and add it to the pipeline
	    if (SIROp instanceof JUnqualifiedInstanceCreation) {
		SIRStream st = (SIRStream)getVisitedOp(((JUnqualifiedInstanceCreation)SIROp).
						       getType().getCClass().getIdent());
		((SIRPipeline)parentStream).add(st);
	    }
	} else if (ident.equals("setDelay")) {
	    if (!(parentStream instanceof SIRFeedbackLoop))
		at.dms.util.Utils.fail("SetDelay called on Non-FeedbackLoop");
	    if (args.length > 1)
		at.dms.util.Utils.fail("Too many args to setDelay");
	    int delay  = ((JIntLiteral)args[0].accept(this)).intValue();
	    ((SIRFeedbackLoop)parentStream).setDelay(delay);
	}
	else if (ident.equals("setBody")) {
	    if (!(parentStream instanceof SIRFeedbackLoop)) 
		at.dms.util.Utils.fail("setBody called on non-FeedbackLoop");
	    if (args.length > 1)
		at.dms.util.Utils.fail("Exactly one arg to setBody() allowed");
	    //Visited the argument
	    JExpression SIROp = (JExpression) args[0].accept(this);
	    //if it is a anonymous creation do nothing, it will be added in visitClassDecl
	    //if it is a named creation, lookup in the symbol table for the visited
	    //node and add it to the pipeline
	    if (SIROp instanceof JUnqualifiedInstanceCreation) {
		SIRStream st = (SIRStream)getVisitedOp(((JUnqualifiedInstanceCreation)SIROp).
						       getType().getCClass().getIdent());
		((SIRFeedbackLoop)parentStream).setBody(st);
	    }
	}
	else if (ident.equals("setLoop")) {
	    if (!(parentStream instanceof SIRFeedbackLoop)) 
		at.dms.util.Utils.fail("setLoop called on non-FeedbackLoop");
	    if (args.length > 1)
		at.dms.util.Utils.fail("Exactly one arg to setLoop() allowed");
	    //Visited the argument
	    JExpression SIROp = (JExpression) args[0].accept(this);
	    //if it is a anonymous creation do nothing, it will be added in visitClassDecl
	    //if it is a named creation, lookup in the symbol table for the visited
	    //node and add it to the pipeline
	    if (SIROp instanceof JUnqualifiedInstanceCreation) {
		SIRStream st = (SIRStream)getVisitedOp(((JUnqualifiedInstanceCreation)SIROp).
						       getType().getCClass().getIdent());
		((SIRFeedbackLoop)parentStream).setLoop(st);
	    }
	}
       
	else if (ident.equals("setSplitter")) {
	    if (!(parentStream instanceof SIRFeedbackLoop))
		at.dms.util.Utils.fail("setSplitter called on non-FeedbackLoop");
	    ((SIRFeedbackLoop)parentStream).setSplitter(buildSplitter(args[0]));
	}
	else if (ident.equals("setJoiner")) {
	    if (!(parentStream instanceof SIRFeedbackLoop))
		 at.dms.util.Utils.fail("setSplitter called on non-FeedbackLoop");
	     ((SIRFeedbackLoop)parentStream).setJoiner(buildJoiner(args[0]));
	}
	else {             //Not an SIR call
	    for (int i = 0; i < args.length; i++)
		args[i] = (JExpression) args[i].accept(this);
	}
	//reset currentMethod
	currentMethod = parentMethod;
	return self;
    }
    

    private SIRSplitter buildSplitter(JExpression type) 
    {
	if (!(type instanceof JMethodCallExpression))
	    at.dms.util.Utils.fail("arg to setSplitter must be a method call");
	int n = 2;
	
	return SIRSplitter.create(parentStream, SIRSplitType.ROUND_ROBIN, 2);
	//return SIRSplitter.create(parentStream, SIRSplitType.DUPLICATE, 2);
    }

    private SIRJoiner buildJoiner(JExpression type) 
    {
	if (!(type instanceof JMethodCallExpression))
	    at.dms.util.Utils.fail("arg to setJoiner must be a method call");
	return SIRJoiner.create(parentStream, SIRJoinType.ROUND_ROBIN, 2);
    }
    
    
    

    /**
   * visits a local variable expression
   */
    public Object visitLocalVariableExpression(JLocalVariableExpression self,
                                             String ident)
    {
        blockStart("LocalVariableExpression");
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
        blockStart("ConditionalExpression");
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
        blockStart("CompoundAssignmentExpression");
	return self;
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
	else return self;
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
	trash = expr.accept(this);
	return self;
    }

    /**
     * visits a cast expression
     */
    public Object visitUnaryPromoteExpression(JUnaryPromote self,
                                            JExpression expr,
                                            CType type)
    {
        blockStart("UnaryPromoteExpression");
	trash = expr.accept(this);
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
        blockStart("BitwiseExpression");
	return self;
    }
    
    /**
     * visits an assignment expression
     */
    public Object visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right)
    {
        blockStart("AssignmentExpression");
        trash = left.accept(this);
        trash = right.accept(this);
	return self;
    }

    /**
     * visits an array length expression
     */
    public Object visitArrayLengthExpression(JArrayLengthExpression self,
                                           JExpression prefix)
    {
        blockStart("ArrayLengthExpression");
        trash = prefix.accept(this);
        return self;
    }

    /**
     * visits an array length expression
     */
    public Object visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor)
    {
        blockStart("ArrayAccessExpression");
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
        blockStart("SwitchLabel");
        trash = expr.accept(this);
        return self;
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
            trash = labels[i].accept(this);
	for (int i = 0; i < stmts.length; i++)
            trash = stmts[i].accept(this);
	return self;
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
            trash = params[i].accept(this);
	return self;
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
            trash = elems[i].accept(this);
        return self;
    }

    /**
     * visits a boolean literal
     */
    public Object visitBooleanLiteral(boolean value)
    {
        blockStart("BooleanLiteral");
	return new Boolean(value);
    }

    /**
     * visits a byte literal
     */
    public Object visitByteLiteral(byte value)
    {
        blockStart("ByteLiteral");
	return new Byte(value);
    }

    /**
     * visits a character literal
     */
    public Object visitCharLiteral(char value)
    {
        blockStart("CharLiteral");
	return new Character(value);
    }

    /**
     * visits a double literal
     */
    public Object visitDoubleLiteral(double value)
    {
        blockStart("DoubleLiteral");
	return new Double(value);
    }

    /**
     * visits a float literal
     */
    public Object visitFloatLiteral(float value)
    {
        blockStart("FloatLiteral");
	return new Float(value);
    }

    /**
     * visits a int literal
     */
    public Object visitIntLiteral(int value)
    {
        blockStart("IntLiteral");
	return new JIntLiteral(null,value);
    }

    /**
     * visits a long literal
     */
    public Object visitLongLiteral(long value)
    {
        blockStart("LongLiteral");
	return new Long(value);
    }

    /**
     * visits a short literal
     */
    public Object visitShortLiteral(short value)
    {
        blockStart("ShortLiteral");
	return new Short(value);
    }

    /**
     * visits a string literal
     */
    public Object visitStringLiteral(String value)
    {
        blockStart("StringLiteral");
	return value;
    }

    /**
     * visits a null literal
     */
    public Object visitNullLiteral()
    {
        blockStart("NullLiteral");
	return null;
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
	/*visitClassBody(new JTypeDeclaration[0], methods, body);*/
	at.dms.util.Utils.fail("Should be no interfaces!");
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
	return null;
    }

    /**
     * visits an array length expression
     */
    public Object visitComment(JavaStyleComment comment) {
	blockStart("Comment");
	return null;
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
