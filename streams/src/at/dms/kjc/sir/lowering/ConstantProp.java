package at.dms.kjc.sir.lowering;

import streamit.scheduler.*;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;

/**
 * This class propagates constants and unrolls loops.  Currently only
 * works for init functions.
 */
public class ConstantProp {

    private ConstantProp() {
    }

    /**
     * Propagates constants as far as possible in <str> and also
     * unrolls loops.
     */
    public static void propagateAndUnroll(SIRStream str) {
	// start at the outermost loop with an empty set of constants
	new ConstantProp().propagateAndUnroll(str, new Hashtable());
    }

    /**
     * Does the work on <str>, given that <constants> maps from
     * a JLocalVariable to a JLiteral for all constants that are known.
     */
    private void propagateAndUnroll(SIRStream str, Hashtable constants) {
	// propagate constants within init function of <str>
	str.getInit().accept(new Propagator(constants));
	// unroll loops within init function of <str>
	//str.getInit().accept(new Unroller(constants));
	// recurse into sub-streams
    }
}

/**
 * This class propagates constants and partially evaluates all
 * expressions as much as possible.
 */
class Propagator extends EmptyAttributeVisitor {
    /**
     * Map of known constants (JLocalVariable -> JLiteral)
     */
    private Hashtable constants;

    /**
     * Creates one of these given that <constants> maps
     * JLocalVariables to JLiterals for the scope that we'll be
     * visiting.
     */
    public Propagator(Hashtable constants) {
	super();
	this.constants = constants;
    }

    // ----------------------------------------------------------------------
    // METHODS AND FIELDS
    // ----------------------------------------------------------------------

    /**
     * Visits a field declaration
     */
    public Object visitFieldDeclaration(JFieldDeclaration self,
					int modifiers,
					CType type,
					String ident,
					JExpression expr) {
	if (expr != null) {
	    expr.accept(this);
	}
	return self;
    }

    // ----------------------------------------------------------------------
    // STATEMENT
    // ----------------------------------------------------------------------

    /**
     * Visits a while statement
     */
    public Object visitWhileStatement(JWhileStatement self,
				      JExpression cond,
				      JStatement body) {
	cond.accept(this);
	body.accept(this);
	return self;
    }

    /**
     * Visits a variable declaration statement
     */
    public Object visitVariableDefinition(JVariableDefinition self,
					  int modifiers,
					  CType type,
					  String ident,
					  JExpression expr) {
	if (expr != null) {
	    expr.accept(this);
	}
	return self;
    }

    /**
     * Visits a switch statement
     */
    public Object visitSwitchStatement(JSwitchStatement self,
				       JExpression expr,
				       JSwitchGroup[] body) {
	expr.accept(this);
	for (int i = 0; i < body.length; i++) {
	    body[i].accept(this);
	}
	return self;
    }

    /**
     * Visits a return statement
     */
    public Object visitReturnStatement(JReturnStatement self,
				       JExpression expr) {
	if (expr != null) {
	    expr.accept(this);
	}
	return self;
    }

    /**
     * Visits a if statement
     */
    public Object visitIfStatement(JIfStatement self,
				   JExpression cond,
				   JStatement thenClause,
				   JStatement elseClause) {
	cond.accept(this);
	thenClause.accept(this);
	if (elseClause != null) {
	    elseClause.accept(this);
	}
	return self;
    }

    /**
     * Visits a for statement
     */
    public Object visitForStatement(JForStatement self,
				    JStatement init,
				    JExpression cond,
				    JStatement incr,
				    JStatement body) {
	if (init != null) {
	    init.accept(this);
	}
	if (cond != null) {
	    cond.accept(this);
	}
	if (incr != null) {
	    incr.accept(this);
	}
	body.accept(this);
	return self;
    }

    /**
     * Visits an expression statement
     */
    public Object visitExpressionStatement(JExpressionStatement self,
					   JExpression expr) {
	expr.accept(this);
	return self;
    }

    /**
     * Visits a do statement
     */
    public Object visitDoStatement(JDoStatement self,
				   JExpression cond,
				   JStatement body) {
	body.accept(this);
	cond.accept(this);
	return self;
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * Visits an unary plus expression
     */
    public Object visitUnaryPlusExpression(JUnaryExpression self,
					   JExpression expr)
    {
	expr.accept(this);
	return self;
    }

    /**
     * Visits an unary minus expression
     */
    public Object visitUnaryMinusExpression(JUnaryExpression self,
					    JExpression expr)
    {
	expr.accept(this);
	return self;
    }

    /**
     * Visits a bitwise complement expression
     */
    public Object visitBitwiseComplementExpression(JUnaryExpression self,
						   JExpression expr)
    {
	expr.accept(this);
	return self;
    }

    /**
     * Visits a logical complement expression
     */
    public Object visitLogicalComplementExpression(JUnaryExpression self,
						   JExpression expr)
    {
	expr.accept(this);
	return self;
    }

    /**
     * Visits a shift expression
     */
    public Object visitShiftExpression(JShiftExpression self,
				       int oper,
				       JExpression left,
				       JExpression right) {
	left.accept(this);
	right.accept(this);
	return self;
    }

    /**
     * Visits a shift expressiona
     */
    public Object visitRelationalExpression(JRelationalExpression self,
					    int oper,
					    JExpression left,
					    JExpression right) {
	left.accept(this);
	right.accept(this);
	return self;
    }

    /**
     * Visits a prefix expression
     */
    public Object visitPrefixExpression(JPrefixExpression self,
					int oper,
					JExpression expr) {
	expr.accept(this);
	return self;
    }

    /**
     * Visits a postfix expression
     */
    public Object visitPostfixExpression(JPostfixExpression self,
					 int oper,
					 JExpression expr) {
	expr.accept(this);
	return self;
    }

    /**
     * Visits a parenthesed expression
     */
    public Object visitParenthesedExpression(JParenthesedExpression self,
					     JExpression expr) {
	expr.accept(this);
	return self;
    }

    /**
     * Visits an unqualified anonymous class instance creation expression.
     */
    public Object visitQualifiedAnonymousCreation(JQualifiedAnonymousCreation self,
						  JExpression prefix,
						  String ident,
						  JExpression[] params,
						  JClassDeclaration decl)
    {
	prefix.accept(this);
	visitArgs(params);
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
	prefix.accept(this);
	visitArgs(params);
	return self;
    }

    /**
     * Visits an unqualified anonymous class instance creation expression.
     */
    public Object visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self,
						    CClassType type,
						    JExpression[] params,
						    JClassDeclaration decl)
    {
	visitArgs(params);
	return self;
    }

    /**
     * Visits an unqualified instance creation expression.
     */
    public Object visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self,
						   CClassType type,
						   JExpression[] params)
    {
	visitArgs(params);
	return self;
    }

    /**
     * Visits an array allocator expression
     */
    public Object visitNewArrayExpression(JNewArrayExpression self,
					  CType type,
					  JExpression[] dims,
					  JArrayInitializer init)
    {
	for (int i = 0; i < dims.length; i++) {
	    if (dims[i] != null) {
		dims[i].accept(this);
	    }
	}
	if (init != null) {
	    init.accept(this);
	}
	return self;
    }

    /**
     * Visits a name expression
     */
    public Object visitNameExpression(JNameExpression self,
				      JExpression prefix,
				      String ident) {
	if (prefix != null) {
	    prefix.accept(this);
	}
	return self;
    }

    /**
     * Visits an array allocator expression
     */
    public Object visitBinaryExpression(JBinaryExpression self,
					String oper,
					JExpression left,
					JExpression right) {
	left.accept(this);
	right.accept(this);
	return self;
    }

    /**
     * Visits a method call expression
     */
    public Object visitMethodCallExpression(JMethodCallExpression self,
					    JExpression prefix,
					    String ident,
					    JExpression[] args) {
	if (prefix != null) {
	    prefix.accept(this);
	}
	visitArgs(args);
	return self;
    }

    /**
     * Visits a local variable expression
     */
    public Object visitLocalVariableExpression(JLocalVariableExpression self,
					       String ident) {
	return self;
    }

    /**
     * Visits an instanceof expression
     */
    public Object visitInstanceofExpression(JInstanceofExpression self,
					    JExpression expr,
					    CType dest) {
	expr.accept(this);
	return self;
    }

    /**
     * Visits an equality expression
     */
    public Object visitEqualityExpression(JEqualityExpression self,
					  boolean equal,
					  JExpression left,
					  JExpression right) {
	left.accept(this);
	right.accept(this);
	return self;
    }

    /**
     * Visits a conditional expression
     */
    public Object visitConditionalExpression(JConditionalExpression self,
					     JExpression cond,
					     JExpression left,
					     JExpression right) {
	cond.accept(this);
	left.accept(this);
	right.accept(this);
	return self;
    }

    /**
     * Visits a compound expression
     */
    public Object visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
						    int oper,
						    JExpression left,
						    JExpression right) {
	left.accept(this);
	right.accept(this);
	return self;
    }

    /**
     * Visits a field expression
     */
    public Object visitFieldExpression(JFieldAccessExpression self,
				       JExpression left,
				       String ident)
    {
	left.accept(this);
	return self;
    }

    /**
     * Visits a cast expression
     */
    public Object visitCastExpression(JCastExpression self,
				      JExpression expr,
				      CType type)
    {
	expr.accept(this);
	return self;
    }

    /**
     * Visits a cast expression
     */
    public Object visitUnaryPromoteExpression(JUnaryPromote self,
					      JExpression expr,
					      CType type)
    {
	expr.accept(this);
	return self;
    }

    /**
     * Visits a compound assignment expression
     */
    public Object visitBitwiseExpression(JBitwiseExpression self,
					 int oper,
					 JExpression left,
					 JExpression right) {
	left.accept(this);
	right.accept(this);
	return self;
    }

    /**
     * Visits an assignment expression
     */
    public Object visitAssignmentExpression(JAssignmentExpression self,
					    JExpression left,
					    JExpression right) {
	left.accept(this);
	right.accept(this);
	return self;
    }

    /**
     * Visits an array length expression
     */
    public Object visitArrayLengthExpression(JArrayLengthExpression self,
					     JExpression prefix) {
	prefix.accept(this);
	return self;
    }

    /**
     * Visits an array length expression
     */
    public Object visitArrayAccessExpression(JArrayAccessExpression self,
					     JExpression prefix,
					     JExpression accessor) {
	prefix.accept(this);
	accessor.accept(this);
	return self;
    }

    // ----------------------------------------------------------------------
    // UTILS
    // ----------------------------------------------------------------------

    /**
     * Visits an array length expression
     */
    public Object visitSwitchLabel(JSwitchLabel self,
				   JExpression expr) {
	if (expr != null) {
	    expr.accept(this);
	}
	return self;
    }

    /**
     * Visits an array length expression
     */
    public Object visitCatchClause(JCatchClause self,
				   JFormalParameter exception,
				   JBlock body) {
	exception.accept(this);
	body.accept(this);
	return self;
    }

    /**
     * Visits an array length expression
     */
    public Object visitArgs(JExpression[] args) {
	if (args != null) {
	    for (int i = 0; i < args.length; i++) {
		args[i].accept(this);
	    }
	}
	return null;
    }

    /**
     * Visits an array length expression
     */
    public Object visitConstructorCall(JConstructorCall self,
				       boolean functorIsThis,
				       JExpression[] params)
    {
	visitArgs(params);
	return self;
    }

    /**
     * Visits an array initializer expression
     */
    public Object visitArrayInitializer(JArrayInitializer self,
					JExpression[] elems)
    {
	for (int i = 0; i < elems.length; i++) {
	    elems[i].accept(this);
	}
	return self;
    }
}

/**
 * This class unrolls loops where it can.
 */
class Unroller extends EmptyAttributeVisitor {
    /**
     * Map of known constants (JLocalVariable -> JLiteral)
     */
    private Hashtable constants;

    /**
     * Creates one of these given that <constants> maps
     * JLocalVariables to JLiterals for the scope that we'll be
     * visiting.
     */
    public Unroller(Hashtable constants) {
	super();
	this.constants = constants;
    }

    // ----------------------------------------------------------------------
    // METHODS AND FIELDS
    // ----------------------------------------------------------------------

    /**
     * Visits a while statement
     */
    public Object visitWhileStatement(JWhileStatement self,
				      JExpression cond,
				      JStatement body) {
	cond.accept(this);
	body.accept(this);
	return self;
    }
}

