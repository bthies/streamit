/*
 * StmtWhile.java: a while loop
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtWhile.java,v 1.1 2002-09-04 15:12:57 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * StmtWhile is a standard while loop.  It has a condition and a loop
 * body.  On entry, the condition is evaluated; if it is true, the body
 * is executed, and the condition evaluated again.  This repeats until
 * the condition is false.
 */
public class StmtWhile extends Statement
{
    Expression cond;
    Statement body;
    
    /** Creates a new while loop. */
    public StmtWhile(FEContext context, Expression cond, Statement body)
    {
        super(context);
        this.cond = cond;
        this.body = body;
    }
    
    /** Returns the loop condition. */
    public Expression getCond()
    {
        return cond;
    }
    
    /** Returns the loop body. */
    public Statement getBody()
    {
        return body;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        // return v.visitStmtWhile(this);
        return null;
    }
}
