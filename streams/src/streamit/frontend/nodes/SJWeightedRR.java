/*
 * SJWeightedRR.java: a weighted round-robin
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: SJWeightedRR.java,v 1.1 2002-09-04 15:12:56 dmaze Exp $
 */

package streamit.frontend.nodes;

import java.util.List;

/**
 * SJWeightedRR is a variable-weight round-robin splitter or joiner.  It
 * has a list of expressions, each of which is a number of items to take or
 * give to a given tape.
 */
public class SJWeightedRR extends SplitterJoiner
{
    private List weights;
    
    /** Creates a new round-robin splitter or joiner with the specified
     * weights. */
    public SJWeightedRR(FEContext context, List weights)
    {
        super(context);
        this.weights = weights;
    }

    /** Returns the list of round-robin weights. */
    public List getWeights()
    {
        return weights;
    }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        // return v.visitSJWeightedRR(this);
        return null;
    }
}
