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
     * Map of known constants/Overloaded for copy prop (JLocalVariable -> JLiteral/JLocalVariableExpr/Array)
     * When storing information about an array JLiteral/JLocalVariablesExpr are stored in the Array being mapped to
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
     * Used for naming constprop vars
     */
    private static int propNum=0;

    /**
     * Parent of all blocks currently being analyzed
     */
    private static JBlock parent;

    /**
     * VerDecls to be added to the parent block
     */
    private static LinkedList parentStatements;

    private static int loopDepth=0;

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
	loopDepth++;
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
	loopDepth--;
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
	    } else if(newExp instanceof JNewArrayExpression) {
		JExpression dim;
		if(((JNewArrayExpression)expr).getDims().length==1) {
		    dim=((JNewArrayExpression)expr).getDims()[0];
		    if(dim instanceof JIntLiteral) {
			constants.put(self,new Object[((JIntLiteral)dim).intValue()]);
			changed.put(self,Boolean.TRUE);
		    }
		}
	    } else if(newExp instanceof JLocalVariableExpression) {
		if(write)
		    self.setExpression(newExp);
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
	    if(body.length>0) {
		Hashtable constants=propagators[0].constants; //Shadow the main constants
		//Remove if value is not same in all switch bodies
		for(int i=1;i<propagators.length;i++) {
		    Propagator prop=propagators[i];
		    LinkedList remove=new LinkedList();
		    Enumeration enum=constants.keys();
		    while(enum.hasMoreElements()) {
			Object key=enum.nextElement();
			if(!(prop.constants.get(key).equals(constants.get(key))))
			    remove.add(key);
		    }
		    for(int j=0;j<remove.size();j++) {
			constants.remove(remove.get(j));
			//changed.put(remove.get(j),Boolean.TRUE);
		    }
		}
		// mark anything that's in <newConstants> but not in
		// <constants> as <changed>
		for (Enumeration e=this.constants.keys(); e.hasMoreElements(); ) {
		    Object key=e.nextElement();
		    if (!constants.containsKey(key)) {
			changed.put(key, Boolean.TRUE);
		    }
		}
		this.constants=constants;
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
	    /*
	    SIRPrinter printer = new SIRPrinter();
	    System.out.println("analyzing the expression");
	    self.accept(printer);
	    printer.close();
	    System.out.println();
	    */
	    
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

	    // propagate through then and else
	    Propagator thenProp=new Propagator((Hashtable)constants.clone(),true);
	    Propagator elseProp=new Propagator((Hashtable)constants.clone(),true);
	    thenClause.accept(thenProp);
	    if (elseClause != null) {
		elseClause.accept(elseProp);
	    }
	    // reconstruct constants as those that are the same in
	    // both <then> and <else>
	    Hashtable newConstants = new Hashtable();
	    for (Enumeration e = thenProp.constants.keys(); e.hasMoreElements(); ) {
		Object thenKey = e.nextElement();
		Object thenVal = thenProp.constants.get(thenKey);
		Object elseVal = elseProp.constants.get(thenKey);
		if (thenVal.equals(elseVal)) {
		    newConstants.put(thenKey, thenVal);
		}
	    }
	    // mark anything that's in <newConstants> but not in
	    // <constants> as <changed>
	    for (Enumeration e = constants.keys(); e.hasMoreElements(); ) {
		Object key = e.nextElement();
		if (!newConstants.containsKey(key)) {
		    changed.put(key, Boolean.TRUE);
		}
	    }
	    // set <constants> to <newConstants>
	    constants = newConstants;
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
	loopDepth++;
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
	loopDepth--;
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
	    if(write) {
		Object val=constants.get(var);
		if(val instanceof JLiteral) {
		    JLiteral lit=(JLiteral)val;
		    if(lit!=null)
			if(lit instanceof JIntLiteral) {
			    constants.put(var,new JIntLiteral(lit.getTokenReference(),((JIntLiteral)lit).intValue()+((self.getOper()==OPE_POSTINC) ? 1 : -1)));
			    return lit;
			}
		}
	    }
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
	    if(write) {
		Object val=constants.get(var);
		if(val instanceof JLiteral) {
		    JLiteral lit=(JLiteral)val;
		    if(lit!=null)
			if(lit instanceof JIntLiteral) {
			    JIntLiteral out=new JIntLiteral(lit.getTokenReference(),((JIntLiteral)lit).intValue()+((self.getOper()==OPE_PREINC) ? 1 : -1));
			    constants.put(var,out);
			    return out;
			}
		}
	    }
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
	} //else
	//System.err.println("WARNING: Compound Assignment of nonvariable: "+left);
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
	if(write&&(newRight!=null))
	    self.setRight(newRight);
        if (newRight.isConstant()) {
	    //System.out.println("Assign: "+left+" "+newRight);
	    if((left instanceof JLocalVariableExpression)&&!propVar(left)) {
		JLocalVariable var=((JLocalVariableExpression)left).getVariable();
		//constants.remove(var);
		constants.put(var,newRight);
		changed.put(var,Boolean.TRUE);
	    } else if(left instanceof JArrayAccessExpression) {
		JExpression expr=((JArrayAccessExpression)left).getPrefix();
		if(expr instanceof JLocalVariableExpression) {
		    JLocalVariable var=((JLocalVariableExpression)expr).getVariable();
		    JExpression accessor=((JArrayAccessExpression)left).getAccessor();
		    Object val=constants.get(var);
		    changed.put(var,Boolean.TRUE);
		    if(val instanceof Object[]) {
			Object[] array=(Object[])val;
			//System.err.println("Array assigned:"+var+"["+accessor+"]="+newRight);
			if(array!=null)
			    if(accessor instanceof JIntLiteral) {
				array[((JIntLiteral)accessor).intValue()]=newRight;
			    } else
				constants.remove(var);
			else
			    constants.remove(var);
		    } else
			constants.remove(var);
		} else if(!(expr instanceof JFieldAccessExpression))
		    System.err.println("WARNING:Cannot Propagate Array Prefix "+expr);
	    }
        } else if((left instanceof JLocalVariableExpression)&&!propVar(left)) {
	    JLocalVariable var=((JLocalVariableExpression)left).getVariable();
	    changed.put(var,Boolean.TRUE);
	    /*if(newRight instanceof JLocalVariableExpression) {
	      //if(propVar(right)) {
	      //if(newRight!=right) {
	      constants.put(var,newRight);
	      constants.put(((JLocalVariableExpression)newRight).getVariable(),newRight);
	      //} else
	      //constants.put(var,right);
	      //} else
	      //constants.remove(var);
	      } else*/ 
	    if(newRight instanceof JNewArrayExpression) {
		JExpression dim;
		if(((JNewArrayExpression)newRight).getDims().length==1) {
		    dim=((JNewArrayExpression)newRight).getDims()[0];
		    if(dim instanceof JIntLiteral)
			constants.put(var,new Object[((JIntLiteral)dim).intValue()]);
		    else
			constants.remove(var);
		} else
		    constants.remove(var);
	    } else if(self.getCopyVar()!=null) {
		constants.put(var,self.getCopyVar());
	    } else {
		constants.remove(var);
	    }
	} else if(left instanceof JArrayAccessExpression) {
	    JExpression expr=((JArrayAccessExpression)left).getPrefix();
	    if(expr instanceof JLocalVariableExpression) {
		JLocalVariable var=((JLocalVariableExpression)expr).getVariable();
		JExpression accessor=((JArrayAccessExpression)left).getAccessor();
		changed.put(var,Boolean.TRUE);
		if(constants.get(var) instanceof Object[]) {
		    Object[] array=(Object[])constants.get(var);
		    if(array!=null)
			if(accessor instanceof JIntLiteral) {
			    /*if(newRight instanceof JLocalVariableExpression) {//&&
				//propVar(newRight)) {
				array[((JIntLiteral)accessor).intValue()]=newRight;
				//System.err.println("Assign:"+var+"["+accessor+"]="+newRight);
				} else*/
			    if(self.getCopyVar()!=null) {
				array[((JIntLiteral)accessor).intValue()]=self.getCopyVar();
				/*if(newRight instanceof JLocalVariableExpression) {
				  constants.put(((JLocalVariableExpression)newRight).getVariable(),newRight);
				  changed.put(var,Boolean.TRUE);
				  }*/
			    }
			} else {
			    constants.remove(var);
			    changed.put(var,Boolean.TRUE);
			}
		    else {
			changed.put(var,Boolean.TRUE);
			constants.remove(var);
		    }
		}
	    } else if(!(expr instanceof JFieldAccessExpression))
		System.err.println("WARNING:Cannot Propagate Array Prefix "+expr);
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
	    return doPromote(expr,expr.convertType(type));
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
	if (constant instanceof JLiteral) {
	    return constant;
	} else if(constant instanceof JLocalVariableExpression) {
	    //if(constant.equals(constants.get(((JLocalVariableExpression)constant).getVariable()))) //Constant has been unchanged
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
	    // return the evaluated relational expression if both sides are constant
	    if (newLeft.isConstant() && newRight.isConstant()) {
		return self.constantFolding();
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
     * Visits an array access expression
     */
    public Object visitArrayAccessExpression(JArrayAccessExpression self,
					     JExpression prefix,
					     JExpression accessor) {
	prefix.accept(this);
	JExpression newExp = (JExpression)accessor.accept(this);
	if(write)
	    if (newExp instanceof JIntLiteral) {
		self.setAccessor(newExp);
		if(prefix instanceof JLocalVariableExpression) {
		    JLocalVariable var=((JLocalVariableExpression)prefix).getVariable();
		    Object val2=constants.get(var);
		    if(val2 instanceof Object[]) {
			Object[] array=(Object[])val2;
			if(array!=null) {
			    //System.err.println("Trying to access index:"+((JIntLiteral)newExp).intValue()+" "+array.length);
			    int index=((JIntLiteral)newExp).intValue(); //fm produces negative indexes for some reason
			    if(index>=0) {
				Object val=array[index];
				//System.err.println("Accessing:"+var+"["+index+"]="+val);
				if(val!=null) {
				    //if(val instanceof JLiteral)
				    return val;
				    //else if(val instanceof JLocalVariableExpression)
				    //if(val.equals(constants.get(((JLocalVariableExpression)val).getVariable()))) //Constant has been unchanged
				    //return val;
				}
			    }
			}
		    } else {
			constants.remove(var);
			changed.put(var,Boolean.TRUE);
		    }
		} else if(!(prefix instanceof JFieldAccessExpression))
		    System.err.println("WARNING:Cannot Propagate Array Prefix "+prefix);
	    }
	return self;
    }

    

    //Breaks up complex assignments
    //Useful for Copy Prop
    public Object visitBlockStatement(JBlock self,JavaStyleComment[] comments) {
	Hashtable copyMap=new Hashtable();
	if(loopDepth<0)
	    System.err.println("Neg Loop Depth!");
	if(loopDepth==0){
	    int size=self.size();
	    for (int i=0;i<size;i++) {
		JStatement state=(JStatement)self.getStatement(i);
		if(state instanceof JExpressionStatement) {
		    JExpression expr=((JExpressionStatement)state).getExpression();
		    if(expr instanceof JAssignmentExpression) {
			//if((((JAssignmentExpression)expr).getCopyVar()==null)) {
			    JExpression left=((JAssignmentExpression)expr).getLeft();
			    JExpression right=((JAssignmentExpression)expr).getRight();
			    //if((!propVar(left))&&(!propVar(right))) {
				//try {
				    //System.err.println("In Assign:"+((JLocalVariableExpression)left).getVariable()+" "+(JFieldAccessExpression)right+" "+((JAssignmentExpression)expr).getCopyVar());
			    //  System.err.println(((JAssignmentExpression)expr).getCopyVar());
				//} catch(Exception e) {
			    /*try {
			      //System.err.println("In Assign:"+left+" "+((JFieldAccessExpression)right)+" "+((JAssignmentExpression)expr).getCopyVar());
			      System.err.println("In Assign:"+left+" "+right+" "+((JAssignmentExpression)expr).getCopyVar());
			      } catch(Exception e3) {
			      }*/
			    /*try {
			      System.err.println("In Assign:"+((JLocalVariableExpression)left).getVariable()+" "+right);
			      } catch(Exception e2) {
			      try {
			      System.err.println("In Assign:"+left+" "+((JLocalVariableExpression)right).getVariable());
			      } catch(Exception e3) {
			      }
			      }*/
				//}
				//if(!(right instanceof JLocalVariableExpression)||!propVar(((JLocalVariableExpression)right).getVariable()))
				//if(left instanceof JArrayAccessExpression) {
				//JExpression prefix=((JArrayAccessExpression)left).getPrefix();
				//if(prefix instanceof JLocalVariableExpression) {
				//if(!propVar(((JLocalVariableExpression)prefix).getVariable())) {
				//CType type;
				//if(right.getType()!=null)
				//	type=right.getType();
				//  else
				//	type=left.getType();
				//  JVariableDefinition var=new JVariableDefinition(self.getTokenReference(),0,type,propName(),right);
				//    self.addStatement(i,new JVariableDeclarationStatement(self.getTokenReference(),var,null));
				//  size++;
				//    i++;
				//  ((JAssignmentExpression)expr).setRight(new JLocalVariableExpression(self.getTokenReference(),var));
				//    }
				//}
				//} else if(left instanceof JLocalVariableExpression) {
				CType type;
				//Types worth copying
				if((right instanceof JFieldAccessExpression)||(right instanceof JArrayAccessExpression)) {
				    if(right.getType()!=null)
					type=right.getType();
				    else
					type=left.getType();
				    JVariableDefinition var=new JVariableDefinition(self.getTokenReference(),0,type,propName(),right);
				    JVariableDeclarationStatement newState=new JVariableDeclarationStatement(self.getTokenReference(),var,null);
				    self.addStatement(i++,newState);
				    size++;
				    ((JAssignmentExpression)expr).setCopyVar(new JLocalVariableExpression(self.getTokenReference(),var));
				//((JAssignmentExpression)expr).setRight(new JLocalVariableExpression(self.getTokenReference(),var));
				}
		    }
		    //}
		    //}
		} else if(state instanceof JVariableDeclarationStatement) {
		    /*JVariableDefinition[] vars=((JVariableDeclarationStatement)state).getVars();
		      if(vars.length==1) {
		      JVariableDefinition var=vars[0];
		      if(propVarLocal(var)&&var.getValue()!=null)
		      copyMap.put(var.getValue(),var);
		      }*/
		}
	    }
	}
	return super.visitBlockStatement(self,comments);
    }
    
    private String propName() {
	return "__constpropvar_"+propNum++;
    }

    private boolean propVarLocal(JLocalVariable var) {
	return var.getIdent().startsWith("__constpropvar_");
    }

    private boolean propVar(Object var) {
	if(!(var instanceof JExpression))
	    System.err.println("WARNING:popVar:"+var);
	if(var instanceof JLocalVariableExpression)
	    return ((JLocalVariableExpression)var).getVariable().getIdent().startsWith("__constpropvar_");
	return false;
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
