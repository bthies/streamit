/*
 * StmtSplit.java: a split statement
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtSplit.java,v 1.2 2002-09-06 16:28:43 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * StmtSplit declares the splitter type for a split-join or feedback
 * loop.
 */
public class StmtSplit extends Statement
{
    private SplitterJoiner sj;
    
    /** Creates a new split statement with the specified splitter type. */
    public StmtSplit(FEContext context, SplitterJoiner splitter)
    {
        super(context);
        sj = splitter;
    }
    
    /** Returns the splitter type for this. */
    public SplitterJoiner getSplitter()
    {
        return sj;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtSplit(this);
    }
}
