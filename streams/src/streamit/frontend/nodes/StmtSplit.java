package streamit.frontend.nodes;

/**
 * Declare the splitter type for a split-join or feedback loop.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StmtSplit.java,v 1.4 2003-07-24 16:58:37 dmaze Exp $
 */
public class StmtSplit extends Statement
{
    private SplitterJoiner sj;
    
    /**
     * Creates a new split statement with the specified splitter type.
     *
     * @param context  file and line number this statement corresponds to
     * @param splitter type of splitter for this stream
     */
    public StmtSplit(FEContext context, SplitterJoiner splitter)
    {
        super(context);
        sj = splitter;
    }
    
    /**
     * Returns the splitter type for this.
     *
     * @return the splitter object
     */
    public SplitterJoiner getSplitter()
    {
        return sj;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtSplit(this);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof StmtSplit))
            return false;
        return ((StmtSplit)other).sj.equals(sj);
    }
    
    public int hashCode()
    {
        return sj.hashCode();
    }
    
    public String toString()
    {
        return "split " + sj;
    }
}
