/*
 * SJDuplicate.java: a duplicating splitter
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: SJDuplicate.java,v 1.1 2002-09-04 15:12:56 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * SJDuplicate is a duplicating splitter.
 */
public class SJDuplicate extends SplitterJoiner
{
    /** Creates a new duplicating splitter. */
    public SJDuplicate(FEContext context)
    {
        super(context);
    }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        // return v.visitSJDuplicate(this);
        return null;
    }
}
