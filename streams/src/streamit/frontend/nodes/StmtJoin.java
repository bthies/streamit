/*
 * StmtJoin.java: a join statement
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtJoin.java,v 1.3 2003-07-24 15:30:53 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * Declare the joiner type for a split-join or feedback loop.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StmtJoin.java,v 1.3 2003-07-24 15:30:53 dmaze Exp $
 */
public class StmtJoin extends Statement
{
    private SplitterJoiner sj;
    
    /**
     * Creates a new join statement with the specified joiner type.
     *
     * @param context  file and line number this statement corresponds to
     * @param splitter type of splitter for this stream
     */
    public StmtJoin(FEContext context, SplitterJoiner joiner)
    {
        super(context);
        sj = joiner;
    }
    
    /**
     * Returns the joiner type for this.
     *
     * @returns the joiner object
     */
    public SplitterJoiner getJoiner()
    {
        return sj;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtJoin(this);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof StmtJoin))
            return false;
        return ((StmtJoin)other).sj.equals(sj);
    }
    
    public int hashCode()
    {
        return sj.hashCode();
    }
    
    public String toString()
    {
        return "join " + sj;
    }
}
