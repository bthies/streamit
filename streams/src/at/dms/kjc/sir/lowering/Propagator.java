package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import java.lang.Math;
import at.dms.compiler.TokenReference;

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
     * Map of constants changed (JLocalVariable -> JLiteral)
     */
    private Hashtable changed;

    /**
     * Determines whether this instance of Propagator writes
     * actual changes or not
     */
    private boolean write;

    /**
     * Creates one of these given that <constants> maps
     * JLocalVariables to JLiterals for the scope that we'll be
     * visiting.
     */
    public Propagator(Hashtable constants) {
	super();
	this.constants = constants;
	changed=new Hashtable();
	write=true;
    }
    
    public Propagator(Hashtable constants,boolean write) {
	super();
	this.constants = constants;
	changed=new Hashtable();
	this.write=write;
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
	if(!write) {
	    cond.accept(this);
	    body.accept(this);
	} else {
	    Propagator newProp=new Propagator((Hashtable)constants.clone(),false);
	    cond.accept(newProp);
	    body.accept(newProp);
	    Enumeration remove=newProp.changed.keys();
	    while(remove.hasMoreElements()) {
		JLocalVariable var=(JLocalVariable)remove.nextElement();
		constants.remove(var);
		changed.put(var,Boolean.TRUE);
	    }
	    Hashtable saveConstants=(Hashtable)constants.clone();
	    JExpression newExp = (JExpression)cond.accept(this);
	    // reset if we found a constant
	    if (newExp.isConstant()) {
		self.setCondition(newExp);
	    }
	    body.accept(this);
	    constants=saveConstants;
	}
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
	    if (newExp.isConstant() /*&& CModifier.contains(modifiers,
				      ACC_FINAL)*/) {
		// reset the value
		if(write)
		    self.setExpression(newExp);
		// remember the value for the duration of our visiting
		constants.put(self, newExp);
		changed.put(self,Boolean.TRUE);
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
	if(!write) {
	    expr.accept(this);
	    for(int i = 0; i < body.length; i++) {
		body[i].accept(this);
	    }
	} else {
	    JExpression newExp = (JExpression)expr.accept(this);
	    // reset if constant
	    if (newExp.isConstant()) {
		self.setExpression(newExp);
	    }
	    Propagator[] propagators=new Propagator[body.length];
	    for (int i = 0; i < body.length; i++) {
		Propagator prop=new Propagator((Hashtable)constants.clone(),true);
		propagators[i]=prop;
		body[i].accept(prop);
	    }
	    if(body.length>0)
		constants=propagators[0].constants;
	    for(int i=1;i<propagators.length;i++) {
		Propagator prop=propagators[i];
		LinkedList remove=new LinkedList();
		Enumeration enum=constants.keys();
		while(enum.hasMoreElements()) {
		    Object key=enum.nextElement();
		    if(!prop.constants.containsKey(key))
			remove.add(key);
		}
		for(int j=0;i<remove.size();j++) {
		    constants.remove(remove.get(j));
		    changed.put(remove.get(j),Boolean.TRUE);
		}
	    }
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
	    if (write&&newExp.isConstant()) {
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
	if(!write) {
	    cond.accept(this);
	    thenClause.accept(this);
	    if(elseClause!=null)
		elseClause.accept(this);
	} else {
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
	    if (elseClause != null) {
		Propagator thenProp=new Propagator((Hashtable)constants.clone(),true);
		thenClause.accept(thenProp);
		Propagator elseProp=new Propagator((Hashtable)constants.clone(),true);
		elseClause.accept(elseProp);
		constants=thenProp.constants;
		Enumeration enum=constants.keys();
		LinkedList remove=new LinkedList();
		while(enum.hasMoreElements()) {
		    Object key=enum.nextElement();
		    if(!elseProp.constants.containsKey(key))
			remove.add(key);
		}
		for(int i=0;i<remove.size();i++) {
		    constants.remove(remove.get(i));
		    changed.put(remove.get(i),Boolean.TRUE);
		}
	    } else {
		thenClause.accept(this);
	    }
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
	//Saving constants to restore them after loop analyzed
	//Hashtable saveConstants=(Hashtable)constants.clone();
	//Recurse first to see if variables are assigned in the loop
	if(!write) {
	    init.accept(this);
	    incr.accept(this);
	    cond.accept(this);
	    body.accept(this);
	} else {
	    Propagator newProp=new Propagator((Hashtable)constants.clone(),false);
	    init.accept(newProp);
	    incr.accept(newProp);
	    cond.accept(newProp);
	    body.accept(newProp);
	    Enumeration remove=newProp.changed.keys();
	    while(remove.hasMoreElements()) {
		JLocalVariable var=(JLocalVariable)remove.nextElement();
		constants.remove(var);
		changed.put(var,Boolean.TRUE);
	    }
	    Hashtable saveConstants=(Hashtable)constants.clone();
	    // cond should never be a constant, or else we have an
	    // infinite or empty loop.  Thus I won't check for it... 
	    // recurse into init
	    JStatement newInit = (JStatement)init.accept(this);
	    if (newInit!=null && newInit!=init) {
		self.setInit(newInit);
	    }
	    
	    write=false;
	    // recurse into incr
	    JStatement newIncr = (JStatement)incr.accept(this);
	    if (newIncr!=null && newIncr!=incr)
		self.setIncr(newIncr);
	    write=true;

	    JExpression newExp = (JExpression)cond.accept(this);
	    if (newExp!=null && newExp!=cond) {
		self.setCond(newExp);
	    }
	    
	    // recurse into body
	    JStatement newBody = (JStatement)body.accept(this);
	    if (newBody!=null && newBody!=body) {
		self.setBody(newBody);
	    }
	    constants=saveConstants;
	}
	return self;
    }

    /**
     * Visits an expression statement
     */
    public Object visitExpressionStatement(JExpressionStatement self,
					   JExpression expr) {
	JExpression newExp = (JExpression)expr.accept(this);
	if (write&&newExp.isConstant()) {
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
	if (write&&newExp.isConstant()) {
	    self.setCondition(newExp);
	}
	return self;
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------
    
    /*public Object visitPeekExpression(SIRPeekExpression self,
		emacs=)		      CType oldTapeType,
				      JExpression oldArg) {
	JExpression newArg=(JExpression)oldArg.accept(this);
	if(newArg.isConstant()) {
	    self.setArg(newArg);
	}
	return self;
	}*/

    
    public Object visitPostfixExpression(JPostfixExpression self,
					int oper,
					JExpression expr) {
	//System.out.println("Operand: "+expr);
	if(expr instanceof JLocalVariableExpression) {
	    JLocalVariable var=((JLocalVariableExpression)expr).getVariable();
	    changed.put(var,Boolean.TRUE);
	    /*if(write) {
	      JLiteral lit=(JLiteral)constants.get(var);
	      if(lit!=null)
	      if(lit instanceof JIntLiteral)
	      constants.put(var,new JIntLiteral(lit.getTokenReference(),((JIntLiteral)lit).intValue()+((self.getOper()==OPE_POSTINC) ? 1 : -1)));
	      return lit;
	      } else*/
	    constants.remove(var);
	} //else
	//System.err.println("WARNING: Postfix of nonvariable: "+expr);
	return self;
    }
    
    public Object visitPrefixExpression(JPrefixExpression self,
					int oper,
					JExpression expr) {
	if(expr instanceof JLocalVariableExpression) {
	    JLocalVariable var=((JLocalVariableExpression)expr).getVariable();
	    changed.put(var,Boolean.TRUE);
	    /*if(write) {
	      JLiteral lit=(JLiteral)constants.get(var);
	      if(lit!=null)
	      if(lit instanceof JIntLiteral) {
	      JIntLiteral out=new JIntLiteral(lit.getTokenReference(),((JIntLiteral)lit).intValue()+((self.getOper()==OPE_POSTINC) ? 1 : -1));
	      constants.put(var,out);
	      return out;
	      }
	      } else*/
	    constants.remove(var);
	} /*else
	    System.err.println("WARNING: Prefix of nonvariable: "+expr);*/
	return self;
    }

    public Object visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
						    int oper,
						    JExpression left,
						    JExpression right) {
	if(left instanceof JLocalVariableExpression) {
	    JLocalVariable var=((JLocalVariableExpression)left).getVariable();
	    constants.remove(var);
	    changed.put(var,Boolean.TRUE);
	} else
	    System.err.println("WARNING: Compound Assignment of nonvariable: "+left);
	JExpression newRight = (JExpression)right.accept(this);
	if (write&&newRight.isConstant())
            self.setRight(newRight);
	return self;
    }
    
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
	    //System.out.println("Assign: "+left+" "+newRight);
	    if(left instanceof JLocalVariableExpression) {
		JLocalVariable var=((JLocalVariableExpression)left).getVariable();
		constants.remove(var);
		//constants.put(var,newRight);
		changed.put(var,Boolean.TRUE);
	    }
        } else
	    if(left instanceof JLocalVariableExpression) {
		JLocalVariable var=((JLocalVariableExpression)left).getVariable();
		constants.remove(var);
		changed.put(var,Boolean.TRUE);
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
	    return new JIntLiteral(newExp.intValue());
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
	    return new JIntLiteral(newExp.intValue()*-1);
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
	if (write&&newExp.isConstant()) {
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
	}
	return self;
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
	if(write) {
	    if (newLeft.isConstant()) {
		self.setLeft(newLeft);
	    }
	    if (newRight.isConstant()) {
		self.setRight(newRight);
	    }
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
	JExpression newRight = (JExpression)right.accept(this);
	if(write) {
	    if (newLeft.isConstant()) {
		self.setLeft(newLeft);
	    }
	    if (newRight.isConstant()) {
		self.setRight(newRight);
	    }
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
	if(write) {
	    if (newLeft.isConstant()) {
		self.setLeft(newLeft);
	    }
	    if (newRight.isConstant()) {
		self.setRight(newRight);
	    }
	}
	// do constant-prop if we have both as constants
	if (write&&newLeft.isConstant() && newRight.isConstant()) {
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
	if(write) {
	    if (newLeft.isConstant()) {
		self.setLeft(newLeft);
	    }
	    if (newRight.isConstant()) {
		self.setRight(newRight);
	    }
	}
	// do constant-prop if we have both as constants
	if (write&&newLeft.isConstant() && newRight.isConstant()) {
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
            if (ident.equals("sin") || ident.equals("cos") ||
                ident.equals("log") || ident.equals("exp")) {
		JExpression narg = doPromote(args[0],
					     new JDoubleLiteral(null, 0.0));
		double darg = narg.doubleValue();
		if (ident.equals("sin"))
		    return new JDoubleLiteral(self.getTokenReference(),
					      Math.sin(darg));
		if (ident.equals("cos"))
		    return new JDoubleLiteral(self.getTokenReference(),
					      Math.cos(darg));
                if (ident.equals("log"))
                    return new JDoubleLiteral(self.getTokenReference(),
                                              Math.log(darg));
                if (ident.equals("exp"))
                    return new JDoubleLiteral(self.getTokenReference(),
                                              Math.exp(darg));
	    }
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
	if (write&&newExp.isConstant()) {
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
