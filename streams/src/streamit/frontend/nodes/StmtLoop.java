/*
 * StmtLoop.java: a body statement
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtLoop.java,v 1.2 2002-09-06 16:28:43 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * StmtLoop adds the loop stream to a feedback loop.  It has
 * a single StreamCreator object that specifies what child is being
 * added.
 */
public class StmtLoop extends Statement
{
    private StreamCreator sc;
    
    /** Creates a new loop statement for a specified child. */
    public StmtLoop(FEContext context, StreamCreator sc)
    {
        super(context);
        this.sc = sc;
    }
    
    /** Returns the child stream creator. */
    public StreamCreator getCreator()
    {
        return sc;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtLoop(this);
    }
}

    
