/*
 * StmtFor.java: a C-style for loop
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtFor.java,v 1.1 2002-09-04 15:12:57 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * StmtFor is a C-style for loop.  The loop contains an initialization
 * statement, a loop condition, and an increment statement.  On entry,
 * the initialization statement is executed, and then the condition is
 * evaluated.  If the condition is true, the body is executed, followed
 * by the increment statement.  This loops until the condition returns
 * false.  Continue statements cause control flow to go to the increment
 * statement.
 */
public class StmtFor extends Statement
{
    private Expression cond;
    private Statement init, incr, body;
    
    /** Creates a new for loop. */
    public StmtFor(FEContext context, Statement init, Expression cond,
                   Statement incr, Statement body)
    {
        super(context);
        this.init = init;
        this.cond = cond;
        this.incr = incr;
        this.body = body;
    }
    
    /** Return the initialization statement of this. */
    public Statement getInit()
    {
        return init;
    }
    
    /** Return the loop condition of this. */
    public Expression getCond()
    {
        return cond;
    }
    
    /** Return the increment statement of this. */
    public Statement getIncr()
    {
        return incr;
    }
    
    /** Return the loop body of this. */
    public Statement getBody()
    {
        return body;
    }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        // return v.acceptStmtFor(this);
        return null;
    }
}

