/*
 * StmtPush.java: a push statement
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtPush.java,v 1.3 2003-06-24 21:40:14 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * StmtPush pushes a single value on to the current filter's output tape.
 * It has an expression, which is the value to be pushed.  The type of
 * the expression must match the output type of the filter exactly.
 */
public class StmtPush extends Statement
{
    Expression value;
    
    /** Creates a new push statement with the specified value. */
    public StmtPush(FEContext context, Expression value)
    {
        super(context);
        this.value = value;
    }

    /** Returns the value this pushes. */
    public Expression getValue()
    {
        return value;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtPush(this);
    }
    
    public boolean equals(Object other)
    {
        if (!(other instanceof StmtPush))
            return false;
        return value.equals(((StmtPush)other).getValue());
    }
    
    public int hashCode()
    {
        return value.hashCode();
    }

    public String toString()
    {
        return "push(" + value + ")";
    }
}
