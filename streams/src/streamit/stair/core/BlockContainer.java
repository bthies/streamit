package streamit.stair.core;

/**
 * A region of a control-flow graph.  This may be an entire function,
 * but it also may be a subset of a graph.  The region has an entry
 * block and an exit block.  There should be a traceable path from the
 * entry to all nodes in the region, and from each node in the region
 * to the exit.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: BlockContainer.java,v 1.1 2003-02-24 21:45:11 dmaze Exp $
 */
public interface BlockContainer
{
    /** Get the entry node of the region. */
    public Block getEntry();
    
    /** Get the exit node of the region. */
    public Block getExit();
}
