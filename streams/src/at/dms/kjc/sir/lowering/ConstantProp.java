package at.dms.kjc.sir.lowering;

import streamit.scheduler.*;

import java.util.*;
import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;

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
	Unroller unroller;
	do {
	    // make a propagator
	    Propagator propagator = new Propagator(constants);
	    // propagate constants within init function of <str>
	    str.getInit().accept(propagator);
	    // propagate into fields of <str>
	    propagateFields(propagator, str);
	    // unroll loops within init function of <str>
	    unroller = new Unroller(constants);
	    str.getInit().accept(unroller);
	    // patch list of children for splitjoins and pipelines
	    if (unroller.hasUnrolled()) {
		if (str instanceof SIRPipeline) {
		    ((SIRPipeline)str).setChildren(GetChildren.
						   getChildren(str));
		} else if (str instanceof SIRSplitJoin) {
		    ((SIRSplitJoin)str).setParallelStreams(GetChildren.
							   getChildren(str));
		}
	    }
	    // iterate until nothing unrolls
	} while (unroller.hasUnrolled());
	// recurse into sub-streams
	recurse(str, constants);
    }

    /**
     * Given a propagator <propagator>, this propagates constants
     * through the fields of <str>.
     */
    private void propagateFields(Propagator propagator, SIRStream str) {
	if (str instanceof SIRFilter) {
	    propagateFilterFields(propagator, (SIRFilter)str);
	} if (str instanceof SIRSplitJoin) {
	    // for split-joins, resolve the weights of splitters and
	    // joiners
	    propagator.visitArgs(((SIRSplitJoin)str).
				 getJoiner().getInternalWeights());
	    propagator.visitArgs(((SIRSplitJoin)str).
				 getSplitter().getInternalWeights());
	} else if (str instanceof SIRFeedbackLoop) {
	    // for feedback loops, resolve the weights of splitters
	    // and joiners
	    propagator.visitArgs(((SIRFeedbackLoop)str).
				 getJoiner().getInternalWeights());
	    propagator.visitArgs(((SIRFeedbackLoop)str).
				 getSplitter().getInternalWeights());
	}
    }

    /**
     * Use <propagator> to propagate constants into the fields of <filter>
     */
    private void propagateFilterFields(Propagator propagator, 
				       SIRFilter filter) {
	// propagate to pop expression
	JExpression newPop = (JExpression)filter.getPop().accept(propagator);
	if (newPop!=null && newPop!=filter.getPop()) {
	    filter.setPop(newPop);
	}
	// propagate to peek expression
	JExpression newPeek = (JExpression)filter.getPeek().accept(propagator);
	if (newPeek!=null && newPeek!=filter.getPeek()) {
	    filter.setPeek(newPeek);
	}
	// propagate to push expression
	JExpression newPush = (JExpression)filter.getPush().accept(propagator);
	if (newPush!=null && newPush!=filter.getPush()) {
	    filter.setPush(newPush);
	}
    }

    /**
     * Recurses from <str> into all its substreams.
     */
    private void recurse(SIRStream str, final Hashtable constants) {
	// if we're at the bottom, we're done
	if (str.getInit()==null) {
	    return;
	}
	// iterate through statements of init function, looking for SIRInit's
	str.getInit().accept(new SLIREmptyVisitor() {
		public void visitInitStatement(SIRInitStatement self,
					       JExpression[] args,
					       SIRStream target) {
		    recurse(self, constants);
		}
	    });
    }

    /**
     * Recurses using <init> given that <constants> were built for
     * the parent.
     */
    private void recurse(SIRInitStatement initStatement, Hashtable constants) {
	// get the init function of the target--this is where analysis
	// will continue
	JMethodDeclaration initMethod = initStatement.getTarget().getInit();
	// if there is no init function, we're done
	if (initMethod==null) {
	    return;
	}
	// otherwise, augment the hashtable mapping the parameters of
	// the init function to any constants that appear in the call...
	// get args to init function
	JExpression[] args = initStatement.getArgs();
	// get parameters of init function
	JFormalParameter[] parameters = initMethod.getParameters();
	// build new constants
	for (int i=0; i<args.length; i++) {
	    // if we are passing an arg to the init function...
	    if (args[i] instanceof JLiteral) {
		// if it's already a literal, just record it
		constants.put(parameters[i], args[i]);
	    } else if (constants.get(args[i])!=null) {
		// otherwise if it's associated w/ a literal, then record that
		constants.put(parameters[i], constants.get(args[i]));
	    }
	}
	// recurse into sub-stream
	propagateAndUnroll(initStatement.getTarget(), constants);
    }
}

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

    /**
     * Visits an array length expression
     */
    public Object visitArrayAccessExpression(JArrayAccessExpression self,
					     JExpression prefix,
					     JExpression accessor) {
	prefix.accept(this);
	JExpression newExp = (JExpression)accessor.accept(this);
	if (newExp.isConstant()) {
	    self.setAccessor(accessor);
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

/**
 * This class unrolls loops where it can.
 */
class Unroller extends SLIRReplacingVisitor {
    /**
     * Map of known constants (JLocalVariable -> JLiteral)
     */
    private Hashtable constants;
    /**
     * Whether or not anything has been unrolled.
     */
    private boolean hasUnrolled;

    /**
     * Creates one of these given that <constants> maps
     * JLocalVariables to JLiterals for the scope that we'll be
     * visiting.
     */
    public Unroller(Hashtable constants) {
	super();
	this.constants = constants;
	this.hasUnrolled = false;
    }

    /**
     * Overload the for-statement visit.
     */
    public Object visitForStatement(JForStatement self,
				    JStatement init,
				    JExpression cond,
				    JStatement incr,
				    JStatement body) {
	// first recurse into body
	JStatement newStmt = (JStatement)body.accept(this);
	if (newStmt!=null && newStmt!=body) {
	    self.setBody(newStmt);
	}
	// check for loop induction variable
	UnrollInfo info = getUnrollInfo(init, cond, incr, body);
	// if we can unroll...
	if (info!=null) {
	    // do unrolling
	    return doUnroll(info, self);
	}
	return self;
    }

    /**
     * Given the loop <self> and original unroll info <info>, perform
     * the unrolling and return a statement block of the new
     * statements.
     */
    private JBlock doUnroll(UnrollInfo info, JForStatement self) {
	// make a list of statements
	List statementList = new LinkedList();
	// get the initial value of the counter
	int counter = info.initVal;
	// simulate execution of the loop...
	while (counter < info.finalVal) {
	    // create new for statement, just to replace the variable
	    JForStatement newSelf 
		= (JForStatement)ObjectDeepCloner.deepCopy(self);
	    // get unroll info for <newSelf>
	    UnrollInfo newInfo = getUnrollInfo(newSelf.getInit(),
					       newSelf.getCondition(),
					       newSelf.getIncrement(),
					       newSelf.getBody());
	    // replace induction variable with its value current value
	    Hashtable newConstants = new Hashtable();
	    newConstants.put(newInfo.var, new JIntLiteral(counter));
	    // do the replacement
	    newSelf.getBody().accept(new Propagator(newConstants));
	    // add to statement list
	    statementList.add(newSelf.getBody());
	    // increment counter
	    counter = incrementCounter(counter, info);
	}
	// mark that we've unrolled
	this.hasUnrolled = true;
	// return new block instead of the for loop
	return new JBlock(null, 
			  (JStatement[])statementList.
			  toArray(new JStatement[0]),
			  null);
    }
    
    /**
     * Given the UnrollInfo <info> and that <counter> was the old
     * value of the count, returns the new value of the count.
     */
    private int incrementCounter(int counter, UnrollInfo info) {
	switch(info.oper) {
	case OPE_PLUS: 
	    return counter + info.incrVal;
	case OPE_STAR: 
	    return counter * info.incrVal;
	default: 
	    Utils.fail("Can only unroll add/mul increments for now.");
	    // dummy value
	    return 0;
	}
    }
    
    /**
     * Return whether or not this has unrolled any loops.
     */
    public boolean hasUnrolled() {
	return hasUnrolled;
    }
    
    /**
     * Gets unroll info for this loop.  Right now, we check that:
     *
     *  1. the initialization is an assignemnt of a constant to a variable
     *      - further, the variable is not declared in the initialization;
     *        it is only assigned to there (no "for (int i=...)")
     *  2. the condition is a relational less-than test of the var and a const
     *  3. the incr is addition or multiplication by a const. (use +=1, not ++)
     *  4. the variable is an integer
     *
     *  We do not check that the induction variable is unmodified in
     *  the loop.  You'll break this if you modify it.
     *
     * This will return <null> if the loop can not be unrolled. 
     */
    private UnrollInfo getUnrollInfo(JStatement init,
				     JExpression cond,
				     JStatement incr,
				     JStatement body) {
	try {
	    // inspect initialization...
	    JAssignmentExpression initExpr 
		= (JAssignmentExpression)
		((JExpressionListStatement)init).getExpression(0);
	    JLocalVariable var 
		= ((JLocalVariableExpression)initExpr.getLeft()).getVariable();
	    int initVal 
		= ((JIntLiteral)initExpr.getRight()).intValue();

	    // inspect condition...
	    JRelationalExpression condExpr = (JRelationalExpression)cond;
	    // make sure variable is the same
	    if (var != 
		((JLocalVariableExpression)condExpr.getLeft()).getVariable()) {
		return null;
	    }
	    // get the upper limit
	    int finalVal = ((JIntLiteral)condExpr.getRight()).intValue();

	    // inspect increment...
	    JCompoundAssignmentExpression incrExpr
		= (JCompoundAssignmentExpression)
		((JExpressionListStatement)incr).getExpression(0);
	    // make sure the variable is the same
	    if (var != 
		((JLocalVariableExpression)incrExpr.getLeft()).getVariable()) {
		return null;
	    }
	    // get the operation
	    int oper = incrExpr.getOperation();
	    // get the increment amount
	    int incrVal = ((JIntLiteral)incrExpr.getRight()).intValue();
	    
	    // return result
	    return new UnrollInfo(var, initVal, finalVal, oper, incrVal);
	} catch (ClassCastException e) {
	    System.out.println("Didn't unroll because:");
	    e.printStackTrace();
	    // assume we failed 'cause assumptions violated -- return null
	    return null;
	}
    }

    /**
     * Returns the number of times that a loop with unroll info <info>
     * can be unrolled.
     */
    private int calcUnrollFactor(UnrollInfo info) {
	switch(info.oper) {
	case OPE_PLUS: 
	    return (info.finalVal-info.initVal)/info.incrVal;
	case OPE_STAR: 
	    // simulate execution of multiplication
	    int count = 0;
	    int val = info.initVal;
	    while (val < info.finalVal) {
		val *= info.incrVal;
		count++;
	    }
	    return count;
	default: 
	    Utils.fail("Can only unroll add/mul increments for now.");
	    // dummy value
	    return 0;
	}
    }

    class UnrollInfo {
	/**
	 * The induction variable in the loop.
	 */
	public final JLocalVariable var;
	/**
	 * The initial value of the induction variable.
	 */
	public final int initVal;
	/**
	 * The final value of the induction variable.
	 */
	public final int finalVal;
	/**
	 * The operation that is being used to change the induction variable.
	 */
	public final int oper;
	/**
	 * The increment.
	 */
	public final int incrVal;
	
	public UnrollInfo(JLocalVariable var,
			  int initVal, 
			  int finalVal, 
			  int oper, 
			  int incrVal) {
	    this.var = var;
	    this.initVal = initVal;
	    this.finalVal = finalVal;
	    this.oper = oper;
	    this.incrVal = incrVal;
	}
    }
}

/**
 * This class is for rebuilding the list of children in a parent
 * stream following unrolling that could have modified the stream
 * structure.
 */
class GetChildren extends SLIREmptyVisitor {
    /**
     * List of children of parent stream.
     */
    private LinkedList children;
    /**
     * The parent stream.
     */
    private SIRStream parent;
        
    /**
     * Makes a new one of these.
     */
    private GetChildren(SIRStream str) {
	this.children = new LinkedList();
	this.parent = str;
    }

    /**
     * Re-inspects the init function of <str> to see who its children
     * are.
     */
    public static LinkedList getChildren(SIRStream str) {
	GetChildren gc = new GetChildren(str);
	if (str.getInit()!=null) {
	    str.getInit().accept(gc);
	}
	return gc.children;
    }

    /**
     * Visits an init statement -- adds <target> to list of children.
     */
    public void visitInitStatement(SIRInitStatement self,
				   JExpression[] args,
				   SIRStream target) {
	// remember <target> as a child
	children.add(target);
	// reset parent of <target> 
	target.setParent((SIRContainer)parent);
    }
}

