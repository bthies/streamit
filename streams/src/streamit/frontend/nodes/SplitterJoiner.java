/*
 * SplitterJoiner.java: base class for StreamIt splitters and joiners
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: SplitterJoiner.java,v 1.1 2002-09-04 15:12:56 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * Base class for all splitters and joiners.
 */
abstract public class SplitterJoiner extends FENode
{
    public SplitterJoiner(FEContext context)
    {
        super(context);
    }
}
