
package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;

/**
 * This class unrolls loops where it can.
 */
class Unroller extends SLIRReplacingVisitor {
    /**
     * Map allowing the current block to access the modified
     * list of the current for loop
     */
    private Hashtable currentModified;
    /**
     * Map of known constants (JLocalVariable -> JLiteral)
     */
    private Hashtable constants;
    /**
     * Holds compile time values
     */
    private Hashtable values;
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
	currentModified=new Hashtable();
	values=new Hashtable();
    }
    
    /**
     * checks prefix
     */
    public Object visitPrefixExpression(JPrefixExpression self,
					int oper,
					JExpression expr) {
	if(expr instanceof JLocalVariableExpression) {
	    currentModified.put(((JLocalVariableExpression)expr).getVariable(),Boolean.TRUE);
	    values.remove(((JLocalVariableExpression)expr).getVariable());
	}
	return super.visitPrefixExpression(self,oper,expr);
    }
    
    /**
     * checks postfix
     */
    public Object visitPostfixExpression(JPostfixExpression self,
					 int oper,
					 JExpression expr) {
	if(expr instanceof JLocalVariableExpression){
	    currentModified.put(((JLocalVariableExpression)expr).getVariable(),Boolean.TRUE);
	    values.remove(((JLocalVariableExpression)expr).getVariable());
	}
	return super.visitPostfixExpression(self,oper,expr);
    }

    /**
     * checks assignment
     */
    public Object visitAssignmentExpression(JAssignmentExpression self,
					    JExpression left,
					    JExpression right) {
	if(left instanceof JLocalVariableExpression) {
	    currentModified.put(((JLocalVariableExpression)left).getVariable(),Boolean.TRUE);
	    if(right instanceof JLiteral)
		values.put(((JLocalVariableExpression)left).getVariable(),right);
	}
	return super.visitAssignmentExpression(self,left,right);
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
	Hashtable saveModified=currentModified;
	currentModified=new Hashtable();
	JStatement newStmt = (JStatement)body.accept(this);
	if (newStmt!=null && newStmt!=body) {
	    self.setBody(newStmt);
	}
	// check for loop induction variable
	
	UnrollInfo info = getUnrollInfo(init, cond, incr, body,values);
	// check to see if var was modified
	// if we can unroll...
	if(info!=null&&(!currentModified.containsKey(info.var))) {
	    // Set modified
	    saveModified.putAll(currentModified);
	    currentModified=saveModified;
	    currentModified.put(info.var,Boolean.TRUE);
	    // do unrolling
	    return doUnroll(info, self);
	}
	saveModified.putAll(currentModified);
	currentModified=saveModified;
	return self;
    }

    /**
     * Returns the number of times a for-loop with the given
     * characteristics will execute, or -1 if the count cannot be
     * determined.
     */
    public static int getNumExecutions(JStatement init,
				       JExpression cond,
				       JStatement incr,
				       JStatement body) {
	UnrollInfo info = getUnrollInfo(init, cond, incr, body,new Hashtable());
	// if we didn't get any unroll info, return -1
	if (info==null) { return -1; }
	// get the initial value of the counter
	int counter = info.initVal;
	// track number of executions
	int result = 0;
	// simulate execution of the loop...
	while (counter < info.finalVal) {
	    // increment counters
	    counter = incrementCounter(counter, info);
	    result++;
	}
	return result;
    }

    /**
     * Given the loop <self> and original unroll info <info>, perform
     * the unrolling and return a statement block of the new
     * statements.
     */
    private JBlock doUnroll(UnrollInfo info, JForStatement self) {
	// make a list of statements
	List statementList = new LinkedList();
	statementList.add(self.getInit());
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
					       newSelf.getBody(),
					       values);
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
	statementList.add(new JExpressionStatement(self.getTokenReference(),new JAssignmentExpression(self.getTokenReference(),new JLocalVariableExpression(self.getTokenReference(),info.var),new JIntLiteral(info.finalVal)),null));
	// mark that we've unrolled
	this.hasUnrolled = true;
	// return new block instead of the for loop
	constants.remove(info.var);
	return new JBlock(null, 
			  (JStatement[])statementList.
			  toArray(new JStatement[0]),
			  null);
    }
    
    /**
     * Given the UnrollInfo <info> and that <counter> was the old
     * value of the count, returns the new value of the count.
     */
    private static int incrementCounter(int counter, UnrollInfo info) {
	switch(info.oper) {
	case OPE_PLUS: 
        case OPE_POSTINC:
        case OPE_PREINC:
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
     *  Above should be fixed --jasperln
     *  2. the condition is a relational less-than test of the var and a const
     *  3. the incr is addition or multiplication or div by a const. (use +=1, not ++)
     *  4. the variable is an integer
     *
     *  We do not check that the induction variable is unmodified in
     *  the loop.  Should be fixed for prefix/suffix expr and assign --jasperln
     *
     * This will return <null> if the loop can not be unrolled. 
     */
    private static UnrollInfo getUnrollInfo(JStatement init,
					    JExpression cond,
					    JStatement incr,
					    JStatement body,
					    Hashtable values) {
	try {
	    JLocalVariable var;
	    int initVal=0;
	    int finalVal=0;
	    // inspect condition...
	    JRelationalExpression condExpr = (JRelationalExpression)cond;
	    var=((JLocalVariableExpression)condExpr.getLeft()).getVariable();
	    if(init instanceof JExpressionListStatement) {
		JAssignmentExpression initExpr 
		    = (JAssignmentExpression)
		    ((JExpressionListStatement)init).getExpression(0);
		if(((JLocalVariableExpression)initExpr.getLeft()).getVariable()==var)
		    initVal 
			= ((JIntLiteral)initExpr.getRight()).intValue();
		else if(values.containsKey(var))
		    initVal=((JIntLiteral)values.get(var)).intValue();
		else
		    throw new Exception("Not Constant!");
	    } else if(values.containsKey(var))
		initVal=((JIntLiteral)values.get(var)).intValue();
	    else
		throw new Exception("Not Constant!");
	    // get the upper limit
	    if(condExpr.getRight() instanceof JIntLiteral)
		finalVal = ((JIntLiteral)condExpr.getRight()).intValue();
	    //else
	    //System.err.println("Cond: "+((JFieldAccessExpression)condExpr.getRight()).isConstant());
	    // inspect increment...
	    int incrVal, oper;
	    JLocalVariableExpression incrVar;
	    JExpression incrExpr;
	    if(incr instanceof JExpressionListStatement)
		incrExpr =
		    ((JExpressionListStatement)incr).getExpression(0);
	    else {
		incrExpr =
		    ((JExpressionStatement)incr).getExpression();
	    }
	    if (incrExpr instanceof JCompoundAssignmentExpression)
		{
		    JCompoundAssignmentExpression cae =
			(JCompoundAssignmentExpression)incrExpr;
		    oper = cae.getOperation();
		    incrVal = ((JIntLiteral)cae.getRight()).intValue();
		    incrVar =
			(JLocalVariableExpression)cae.getLeft();
		}
	    else if(incrExpr instanceof JAssignmentExpression){
		JAssignmentExpression ae=(JAssignmentExpression)incrExpr;
		//oper = ae.getOperation();
		//incrVal=((JIntLiteral)ae.getRight()).intValue();
		incrVar=(JLocalVariableExpression)ae.getLeft();
		System.err.println("Right:"+ae.getRight());
		JBinaryExpression expr=(JBinaryExpression)ae.getRight();
		if(expr instanceof JDivideExpression) {
		    if(!((JLocalVariableExpression)expr.getLeft()).equals(incrVar)) {
			//System.err.println("Vars don't match!");
			return null;
		    }
		    incrVal=((JIntLiteral)expr.getRight()).intValue();
		    oper=OPE_SLASH;
		} else
		    return null;
	    } else if (incrExpr instanceof JPrefixExpression)
		{
		    JPrefixExpression pfx = (JPrefixExpression)incrExpr;
		    oper = pfx.getOper();
		    incrVal = 1;
		    incrVar = (JLocalVariableExpression)pfx.getExpr();
		}
	    else if (incrExpr instanceof JPostfixExpression)
		{
		    JPostfixExpression pfx = (JPostfixExpression)incrExpr;
		    oper = pfx.getOper();
		    incrVal = 1;
		    incrVar = (JLocalVariableExpression)pfx.getExpr();
		}
	    else
		return null;
	    
	    // make sure the variable is the same
	    if (var != incrVar.getVariable())
		return null;
	    
	    // return result
	    return new UnrollInfo(var, initVal, finalVal, oper, incrVal);
	} catch (Exception e) {
	    // uncommment these lines if you want to trace a case of something
	    // not unrolling ---
	    
	    //System.err.println("Didn't unroll because:");
	    //e.printStackTrace();
	    
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
        case OPE_POSTINC:
        case OPE_PREINC:
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
	case OPE_SLASH:
	    // simulate execution of division
	    int count2 = 0;
	    int val2 = info.initVal;
	    while (val2 > info.finalVal) {
		val2 /= info.incrVal;
		count2++;
	    }
	    return count2;
	default: 
	    Utils.fail("Can only unroll add/mul/div increments for now.");
	    // dummy value
	    return 0;
	}
    }

    static class UnrollInfo {
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

