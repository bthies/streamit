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

    public Kopi2SIR() {
	parentStream = null;
	topLevel = null;
	parentOperator = null;
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
	}
	if (exp.getIdent().startsWith("pop")) {
	    newExp = new SIRPopExpression();
	}
	if (exp.getIdent().startsWith("peek")) {
	    newExp = new SIRPeekExpression(args[0]);
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
	if (me.getParent() instanceof SIRPipeline)
	    ((SIRPipeline)me.getParent()).add(me);
	else
	    at.dms.util.Utils.fail("Unimplemented SIRStream (cannot register)");
    }
    
    private void buildInit(SIRPipeline node) {
	JStatement[] initStatements = new JStatement[node.size()];
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

    private void postVisit(SIROperator current) {
	if (current instanceof SIRStream)
	    registerWithParent((SIRStream)current);
	if (current instanceof SIRPipeline) 
	    buildInit((SIRPipeline) current);
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

	num++;

	if (self.getSourceClass() != null)
	    printMe("In " + 
			       self.getSourceClass().getSuperClass().getIdent() + num);
	
	// create a new SIROperator
	current = newSIROP(self);

	//If there is no toplevel stream set, set it, this is it!
	if (topLevel == null)
	    topLevel = (SIRStream) current;
	
	if (current == null) 
	    printMe("Null");
	
	trash = visitClassBody(decls, methods, body);

	/* add the current SIR OP to the parent */
	
	if (current == null) 
	    printMe("Null");
	
	printMe( "Out " + self.getSourceClass().getSuperClass().getIdent()
			    + num);

	/* Perform any operations needed after the childern are visited */
	postVisit(current);
	
	parentStream = oldParentStream;
	parentOperator = oldParentOperator;
	
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
	    String channelType = (String)v.elementAt(0);
	    if (channelType.equals("Integer"))
		filter.setInputType(CStdType.Integer);
	    else
		at.dms.util.Utils.fail("Non-Supported Type for Filter Input");
	    filter.setPop(((Integer)v.elementAt(1)).intValue());
	    if (v.size() > 2) {
		if (((Integer)v.elementAt(2)).intValue() < 
		    ((Integer)v.elementAt(1)).intValue())
		    at.dms.util.Utils.fail("Peeks less than Pops!");
		filter.setPeek(((Integer)v.elementAt(2)).intValue());
	    }
	    else
		filter.setPeek(((Integer)v.elementAt(1)).intValue());
	    return self;
	}
	
	/* Output declaration set the fields for the current
	   stream */ 
	else if (ident.equals("output")) {
	    if (!(parentStream instanceof SIRFilter))
		at.dms.util.Utils.fail("Output declaration on non-Filter");
	    SIRFilter filter = (SIRFilter)parentStream;
	    Vector v = (Vector)expr.accept(this);
	    String channelType = (String)v.elementAt(0);
	    int push = ((Integer)v.elementAt(1)).intValue();
	    filter.setPush(push);
	    if (channelType.equals("Integer"))
		filter.setOutputType(CStdType.Integer);
	    else
		at.dms.util.Utils.fail("Non-Supported Type for Filter Output");
	    return self;
	}
	else {   /*Normal field declaration, add this field */
	    parentStream.addField(self);
	}

	if (expr != null) 
	    trash = expr.accept(this);
	return self;
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
	    body[i] = (JStatement)body[i].accept(this);
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
        trash = expr.accept(this);   
	return self;
    }

    /**
     * visits an unary minus expression
     */
    public Object visitUnaryMinusExpression(JUnaryExpression self,
                                          JExpression expr)
    {
        blockStart("UnaryMinusExpression");
        trash = expr.accept(this);
	return self;
    }

    /**
     * visits a bitwise complement expression
     */
    public Object visitBitwiseComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        blockStart("BitwiseComplementExpression");
        trash = expr.accept(this);
	return self;
    }

    /**
     * visits a logical complement expression
     */
    public Object visitLogicalComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        blockStart("LogicalComplementExpression");
        trash = expr.accept(this);
	return self;
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
        blockStart("RelationalExpression");
	return self;
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
        trash = expr.accept(this);
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
	if (params.length == 0)
	    params = JExpression.EMPTY;
	if (type.getIdent().equals("Channel")) 
	    /*Channel declaration, treat the args as special */
	{
	    String channelType = (String)params[0].accept(this);
	    Integer num = (Integer)params[1].accept(this);
	    Vector v = new Vector(3);
	    v.add(channelType);
	    v.add(num);
	    Integer num2;
	    if (params.length > 2) { 
		num2 = (Integer)params[2].accept(this);
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
	trash = left.accept(this);
        trash = right.accept(this);
	return self;
    }

    /**
     * visits a method call expression
     */
    public Object visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args)
    {
	blockStart("MethodCallExpression: " + self.getIdent());
        
	if (isSIRExp(self)) {
	    printMe("SIR Expression " + ident);
	    return newSIRExp(self, args);
	}
	else {             //Not an SIR call
	    for (int i = 0; i < args.length; i++)
		args[i] = (JExpression) args[i].accept(this);
	}
	return self;
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
	return left.getType().getCClass().getIdent();
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
	return null;
    }

    /**
     * visits a byte literal
     */
    public Object visitByteLiteral(byte value)
    {
        blockStart("ByteLiteral");
	return null;
    }

    /**
     * visits a character literal
     */
    public Object visitCharLiteral(char value)
    {
        blockStart("CharLiteral");
	return null;
    }

    /**
     * visits a double literal
     */
    public Object visitDoubleLiteral(double value)
    {
        blockStart("DoubleLiteral");
	return null;
    }

    /**
     * visits a float literal
     */
    public Object visitFloatLiteral(float value)
    {
        blockStart("FloatLiteral");
	return null;
    }

    /**
     * visits a int literal
     */
    public Object visitIntLiteral(int value)
    {
        blockStart("IntLiteral");
	return new Integer(value);
    }

    /**
     * visits a long literal
     */
    public Object visitLongLiteral(long value)
    {
        blockStart("LongLiteral");
	return null;
    }

    /**
     * visits a short literal
     */
    public Object visitShortLiteral(short value)
    {
        blockStart("ShortLiteral");
	return null;
    }

    /**
     * visits a string literal
     */
    public Object visitStringLiteral(String value)
    {
        blockStart("StringLiteral");
	return null;
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
	return comments;
    }

    /**
     * visits an array length expression
     */
    public Object visitComment(JavaStyleComment comment) {
	return comment;
    }

    /**
     * visits an array length expression
     */
    public Object visitJavadoc(JavadocComment comment) {
	return comment;
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
