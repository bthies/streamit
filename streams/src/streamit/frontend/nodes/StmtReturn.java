/*
 * StmtReturn.java: a return statement
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtReturn.java,v 1.1 2002-09-04 15:12:57 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * StmtReturn is a return statement with an optional value.  Functions
 * returning void (including init and work functions and message handlers)
 * should have return statements with no value; helper functions returning a
 * particular type should have return statements with expressions of that
 * type.
 */
public class StmtReturn extends Statement
{
    Expression value;
    
    /** Creates a new return statement, with the specified return value
     * (or null). */
    public StmtReturn(FEContext context, Expression value)
    {
        super(context);
        this.value = value;
    }

    /** Returns the return value of this, or null if there is no return
     * value. */
    public Expression getValue()
    {
        return value;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        // return v.visitStmtReturn();
        return null;
    }
}
