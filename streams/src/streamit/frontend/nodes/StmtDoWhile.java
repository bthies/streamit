/*
 * StmtDoWhile.java: a while loop
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtDoWhile.java,v 1.1 2002-09-04 15:12:57 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * StmtDoWhile is a standard do-while loop.  It has a loop body and a
 * condition.  On entry, the body is executed, and then the condition
 * is evaluated; if it is true, the body is executed again.  This continues
 * until the condition evaluates to false.
 */
public class StmtDoWhile extends Statement
{
    Statement body;
    Expression cond;
    
    /** Creates a new while loop. */
    public StmtDoWhile(FEContext context, Statement body, Expression cond)
    {
        super(context);
        this.body = body;
        this.cond = cond;
    }
    
    /** Returns the loop body. */
    public Statement getBody()
    {
        return body;
    }
    
    /** Returns the loop condition. */
    public Expression getCond()
    {
        return cond;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        // return v.visitStmtDoWhile(this);
        return null;
    }
}
