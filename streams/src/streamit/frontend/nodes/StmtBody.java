/*
 * StmtBody.java: a body statement
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtBody.java,v 1.1 2002-09-04 15:12:56 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * StmtBody adds the body stream to a feedback loop.  It has
 * a single StreamCreator object that specifies what child is being
 * added.
 */
public class StmtBody extends Statement
{
    private StreamCreator sc;
    
    /** Creates a new body statement for a specified child. */
    public StmtBody(FEContext context, StreamCreator sc)
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
        // return v.visitStmtBody(this);
        return null;
    }
}

    
