/*
 * FENode.java: any node in the StreamIt front-end
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: FENode.java,v 1.1 2002-08-19 20:48:14 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * An FENode is any node in the tree created by the front-end's parser.
 * These include statement, expression, and stream object nodes.
 */
public abstract class FENode
{
    private FEContext context;
    
    /** Create a new node with the specified context. */
    public FENode(FEContext context)
    {
        this.context = context;
    }
    
    /** Returns the context associated with this node. */
    public FEContext getContext()
    {
        return context;
    }

    /** Calls an appropriate method in a visitor object with this as
     * a parameter. */
    abstract public Object accept(FEVisitor v);
}
