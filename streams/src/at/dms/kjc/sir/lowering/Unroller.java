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
            int incrVal, oper;
            JLocalVariableExpression incrVar;
            JExpression incrExpr =
                ((JExpressionListStatement)incr).getExpression(0);
            if (incrExpr instanceof JCompoundAssignmentExpression)
            {
                JCompoundAssignmentExpression cae =
                    (JCompoundAssignmentExpression)incrExpr;
                oper = cae.getOperation();
                incrVal = ((JIntLiteral)cae.getRight()).intValue();
                incrVar =
                    (JLocalVariableExpression)cae.getLeft();
            }
            else if (incrExpr instanceof JPrefixExpression)
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
	} catch (ClassCastException e) {
	    // uncommment these lines if you want to trace a case of something
	    // not unrolling ---
	    /*
	      System.out.println("Didn't unroll because:");
	      e.printStackTrace();
	    */
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

