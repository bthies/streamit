/*
 * StmtJoin.java: a join statement
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtJoin.java,v 1.2 2002-09-06 16:28:43 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * StmtJoin declares the joiner type for a split-join or feedback
 * loop.
 */
public class StmtJoin extends Statement
{
    private SplitterJoiner sj;
    
    /** Creates a new join statement with the specified joiner type. */
    public StmtJoin(FEContext context, SplitterJoiner joiner)
    {
        super(context);
        sj = joiner;
    }
    
    /** Returns the joiner type for this. */
    public SplitterJoiner getJoiner()
    {
        return sj;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtJoin(this);
    }
}
