/*
 * SJRoundRobin.java: a fixed-weight round-robin
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: SJRoundRobin.java,v 1.2 2002-09-06 16:28:43 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * SJRoundRobin is a fixed-weight round-robin splitter or joiner.  It
 * has a single expression, which is the number of items to take or
 * give to each tape.
 */
public class SJRoundRobin extends SplitterJoiner
{
    private Expression weight;
    
    /** Creates a new round-robin splitter or joiner with the specified
     * weight. */
    public SJRoundRobin(FEContext context, Expression weight)
    {
        super(context);
        this.weight = weight;
    }

    /** Creates a new round-robin splitter or joiner with weight 1. */
    public SJRoundRobin(FEContext context)
    {
        this(context, new ExprConstInt(context, 1));
    }

    /** Returns the number of items distributed to or from each tape. */
    public Expression getWeight()
    {
        return weight;
    }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitSJRoundRobin(this);
    }
}
