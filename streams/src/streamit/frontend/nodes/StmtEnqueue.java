/*
 * StmtEnqueue.java: an enqueue statement
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtEnqueue.java,v 1.2 2002-09-06 16:28:43 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * StmtEnqueue pushes a single value on to the feedback input of the
 * current feedback loop's joiner.  It has an expression, which is the
 * value to be enqueued.  The type of the expression must match the
 * output type of the loop stream exactly; this is also the same type
 * as the input type of the feedback loop, unless that type is void.
 */
public class StmtEnqueue extends Statement
{
    Expression value;
    
    /** Creates a new enqueue statement with the specified value. */
    public StmtEnqueue(FEContext context, Expression value)
    {
        super(context);
        this.value = value;
    }

    /** Returns the value this enqueues. */
    public Expression getValue()
    {
        return value;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtEnqueue(this);
    }
}
