package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import java.lang.Math;

/**
 * This class propagates constants and partially evaluates all
 * expressions as much as possible.
 */
class Propagator extends SLIRReplacingVisitor {
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
    // STATEMENT
    // ----------------------------------------------------------------------

    /**
     * Visits a while statement
     */
    public Object visitWhileStatement(JWhileStatement self,
				      JExpression cond,
				      JStatement body) {
	JExpression newExp = (JExpression)cond.accept(this);
	// reset if we found a constant
	if (newExp.isConstant()) {
	    self.setCondition(newExp);
	}
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
	    JExpression newExp = (JExpression)expr.accept(this);
	    // if we have a constant AND it's a final variable...
	    if (newExp.isConstant() && CModifier.contains(modifiers,
							  ACC_FINAL)) {
		// reset the value
		self.setExpression(newExp);
		// remember the value for the duration of our visiting
		constants.put(self, newExp);
	    }
	}
	return self;
    }

    /**
     * Visits a switch statement
     */
    public Object visitSwitchStatement(JSwitchStatement self,
				       JExpression expr,
				       JSwitchGroup[] body) {
	JExpression newExp = (JExpression)expr.accept(this);
	// reset if constant
	if (newExp.isConstant()) {
	    self.setExpression(newExp);
	}
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
	    JExpression newExp = (JExpression)expr.accept(this);
	    if (newExp.isConstant()) {
		self.setExpression(newExp);
	    }
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
	JExpression newExp = (JExpression)cond.accept(this);
	if (newExp.isConstant()) {
	    self.setCondition(newExp);
	}
        if (newExp instanceof JBooleanLiteral)
        {
            JBooleanLiteral bval = (JBooleanLiteral)newExp;
            if (bval.booleanValue())
                return thenClause.accept(this);
            else if (elseClause != null)
                return elseClause.accept(this);
            else
                return new JEmptyStatement(self.getTokenReference(), null);
        }
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
	// cond should never be a constant, or else we have an
	// infinite or empty loop.  Thus I won't check for it... 
	return super.visitForStatement(self, init, cond, incr, body);
    }

    /**
     * Visits an expression statement
     */
    public Object visitExpressionStatement(JExpressionStatement self,
					   JExpression expr) {
	JExpression newExp = (JExpression)expr.accept(this);
	if (newExp.isConstant()) {
	    self.setExpression(newExp);
	}
	return self;
    }

    /**
     * Visits a do statement
     */
    public Object visitDoStatement(JDoStatement self,
				   JExpression cond,
				   JStatement body) {
	body.accept(this);
	JExpression newExp = (JExpression)cond.accept(this);
	if (newExp.isConstant()) {
	    self.setCondition(newExp);
	}
	return self;
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * Visits an assignment expression
     */
    public Object visitAssignmentExpression(JAssignmentExpression self,
                                            JExpression left,
                                            JExpression right)
    {
        left.accept(this);
        JExpression newRight = (JExpression)right.accept(this);
        if (newRight.isConstant()) {
            self.setRight(newRight);
        }
        return self;
    }

    /**
     * Visits an unary plus expression
     */
    public Object visitUnaryPlusExpression(JUnaryExpression self,
					   JExpression expr)
    {
	JExpression newExp = (JExpression)expr.accept(this);
	if (newExp.isConstant()) {
	    return new JIntLiteral(newExp.intValue()+1);
	} else {
	    return self;
	}
    }

    /**
     * visits a cast expression
     */
    public Object visitCastExpression(JCastExpression self,
				      JExpression expr,
				      CType type) {
	JExpression newExp = (JExpression)expr.accept(this);
	// return a constant if we have it
	if (newExp.isConstant()) {
	    return newExp;
	} else {
	    return self;
	}
    }

    /**
     * Visits an unary minus expression
     */
    public Object visitUnaryMinusExpression(JUnaryExpression self,
					    JExpression expr)
    {
	JExpression newExp = (JExpression)expr.accept(this);
	if (newExp.isConstant()) {
	    return new JIntLiteral(newExp.intValue()-1);
	} else {
	    return self;
	}
    }

    /**
     * Visits a bitwise complement expression
     */
    public Object visitBitwiseComplementExpression(JUnaryExpression self,
						   JExpression expr)
    {
	JExpression newExp = (JExpression)expr.accept(this);
	if (newExp.isConstant()) {
	    return new JIntLiteral(~newExp.intValue());
	} else {
	    return self;
	}
    }

    /**
     * Visits a logical complement expression
     */
    public Object visitLogicalComplementExpression(JUnaryExpression self,
						   JExpression expr)
    {
	JExpression newExp = (JExpression)expr.accept(this);
	if (newExp.isConstant()) {
	    return new JBooleanLiteral(null, !newExp.booleanValue());
	} else {
	    return self;
	}
    }

    /**
     * Visits a shift expression
     */
    public Object visitShiftExpression(JShiftExpression self,
				       int oper,
				       JExpression left,
				       JExpression right) {
	JExpression newLeft = (JExpression)left.accept(this);
	JExpression newRight = (JExpression)right.accept(this);
	if (newLeft.isConstant() && newRight.isConstant()) {
	    switch (oper) {
	    case OPE_SL:
		return new JIntLiteral(newLeft.intValue() << 
				       newRight.intValue());
	    case OPE_SR:
		return new JIntLiteral(newLeft.intValue() >>
				       newRight.intValue());
	    case OPE_BSR:
		return new JIntLiteral(newLeft.intValue() >>>
				       newRight.intValue());
	    default:
		throw new InconsistencyException();
	    }
	} else {
	    return self;
	}
    }

    /**
     * Visits a parenthesed expression
     */
    public Object visitParenthesedExpression(JParenthesedExpression self,
					     JExpression expr) {
	JExpression newExp = (JExpression)expr.accept(this);
	if (newExp.isConstant()) {
	    self.setExpression(newExp);
	}
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
		JExpression newExp = (JExpression)dims[i].accept(this);
		if (newExp.isConstant()) {
		    dims[i] = newExp;
		}
	    }
	}
	if (init != null) {
	    init.accept(this);
	}
	return self;
    }

    /**
     * Visits a local variable expression
     */
    public Object visitLocalVariableExpression(JLocalVariableExpression self,
					       String ident) {
	// if we know the value of the variable, return a literal.
	// otherwise, just return self
	Object constant = constants.get(self.getVariable());
	if (constant!=null) {
	    return constant;
	} else {
	    return self;
	}
    }

    /**
     * Visits a relational expression
     */
    public Object visitRelationalExpression(JRelationalExpression self,
					    int oper,
					    JExpression left,
					    JExpression right) {
	JExpression newLeft = (JExpression) left.accept(this);
	JExpression newRight = (JExpression) right.accept(this);
	if (newLeft.isConstant()) {
	    self.setLeft(newLeft);
	}
	if (newRight.isConstant()) {
	    self.setRight(newRight);
	}
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
	JExpression newLeft = (JExpression)left.accept(this);
	if (newLeft.isConstant()) {
	    self.setLeft(newLeft);
	}
	JExpression newRight = (JExpression)right.accept(this);
	if (newRight.isConstant()) {
	    self.setRight(newRight);
	}
	return self;
    }

    /**
     * Visits a binary expression
     */
    public Object visitBinaryExpression(JBinaryExpression self,
					String oper,
					JExpression left,
					JExpression right) {
	if (self instanceof JBinaryArithmeticExpression) {
	    return doBinaryArithmeticExpression((JBinaryArithmeticExpression)
						self, 
						left, 
						right);
	} else {
	    return self;
	}
    }

    /**
     * Visits a compound assignment expression
     */
    public Object visitBitwiseExpression(JBitwiseExpression self,
					 int oper,
					 JExpression left,
					 JExpression right) {
	return doBinaryArithmeticExpression(self, left, right);
    }

    /**
     * For processing BinaryArithmeticExpressions.  
     */
    private Object doBinaryArithmeticExpression(JBinaryArithmeticExpression 
						self,
						JExpression left,
						JExpression right) {
	JExpression newLeft = (JExpression)left.accept(this);
	JExpression newRight = (JExpression)right.accept(this);
        // promote constants if needed
        newLeft = doPromote(newLeft, newRight);
        newRight = doPromote(newRight, newLeft);
	// set any constants that we have (just to save at runtime)
	if (newLeft.isConstant()) {
	    self.setLeft(newLeft);
	}
	if (newRight.isConstant()) {
	    self.setRight(newRight);
	}
	// do constant-prop if we have both as constants
	if (newLeft.isConstant() && newRight.isConstant()) {
	    return self.constantFolding();
	} else {
	    // otherwise, return self
	    return self;
	}
    }

    public Object visitEqualityExpression(JEqualityExpression self,
                                          boolean equal,
                                          JExpression left,
                                          JExpression right) {
        // Wow, doesn't Kopi just suck?  This is the *exact* same code
        // as in doBinaryArithmeticExpression, but JEqualityExpression
        // is a different type.
	JExpression newLeft = (JExpression)left.accept(this);
	JExpression newRight = (JExpression)right.accept(this);
        // promote constants if needed
        newLeft = doPromote(newLeft, newRight);
        newRight = doPromote(newRight, newLeft);
	// set any constants that we have (just to save at runtime)
	if (newLeft.isConstant()) {
	    self.setLeft(newLeft);
	}
	if (newRight.isConstant()) {
	    self.setRight(newRight);
	}
	// do constant-prop if we have both as constants
	if (newLeft.isConstant() && newRight.isConstant()) {
	    return self.constantFolding();
	} else {
	    // otherwise, return self
	    return self;
	}
    }

    private JExpression doPromote(JExpression from, JExpression to)
    {
        if (from instanceof JFloatLiteral && to instanceof JDoubleLiteral)
            return new JDoubleLiteral(from.getTokenReference(),
                                      from.floatValue());
        if (from instanceof JIntLiteral && to instanceof JFloatLiteral)
            return new JFloatLiteral(from.getTokenReference(),
                                     from.intValue());
        if (from instanceof JIntLiteral && to instanceof JDoubleLiteral)
            return new JDoubleLiteral(from.getTokenReference(),
				      from.intValue());
        return from;
    }

    /**
     * Visits a method call expression.  Simplifies known idempotent
     * functions.
     */
    public Object visitMethodCallExpression(JMethodCallExpression self,
                                            JExpression prefix,
                                            String ident,
                                            JExpression[] args)
    {
        prefix.accept(this);
        for (int i = 0; i < args.length; i++)
            args[i] = (JExpression)args[i].accept(this);

        // Look for known idempotent functions.
        if (args.length == 1 && args[0].isConstant())
        {
            JExpression narg = doPromote(args[0],
                                         new JDoubleLiteral(null, 0.0));
            double darg = narg.doubleValue();
            if (ident.equals("sin"))
                return new JDoubleLiteral(self.getTokenReference(),
                                          Math.sin(darg));
            if (ident.equals("cos"))
                return new JDoubleLiteral(self.getTokenReference(),
                                          Math.cos(darg));
        }
        return self;
    }

    /**
     * Visits an array length expression
     */
    public Object visitArrayAccessExpression(JArrayAccessExpression self,
					     JExpression prefix,
					     JExpression accessor) {
	prefix.accept(this);
	JExpression newExp = (JExpression)accessor.accept(this);
	if (newExp.isConstant()) {
	    self.setAccessor(newExp);
	}
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
	    JExpression newExp = (JExpression)expr.accept(this);
	    if (newExp.isConstant()) {
		self.setExpression(newExp);
	    }
	}
	return self;
    }

    /**
     * Visits a set of arguments
     */
    public Object visitArgs(JExpression[] args) {
	if (args != null) {
	    for (int i = 0; i < args.length; i++) {
		JExpression newExp = (JExpression)args[i].accept(this);
		if (newExp.isConstant()) {
		    args[i] = newExp;
		}
	    }
	}
	return null;
    }

}
