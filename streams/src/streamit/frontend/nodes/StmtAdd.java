/*
 * StmtAdd.java: an add statement
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtAdd.java,v 1.1 2002-09-04 15:12:56 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * StmtAdd adds a child stream to a pipeline or split-join.  It has
 * a single StreamCreator object that specifies what child is being
 * added.
 */
public class StmtAdd extends Statement
{
    private StreamCreator sc;
    
    /** Creates a new add statement for a specified child. */
    public StmtAdd(FEContext context, StreamCreator sc)
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
        // return v.visitStmtAdd(this);
        return null;
    }
}

    
