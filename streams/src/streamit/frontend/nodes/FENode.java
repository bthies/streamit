/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.frontend.nodes;

/**
 * Any node in the tree created by the front-end's parser.  This is
 * the root of the front-end class tree.  Derived classes include
 * statement, expression, and stream object nodes.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: FENode.java,v 1.2 2003-10-09 19:50:59 dmaze Exp $
 */
public abstract class FENode
{
    private FEContext context;
    
    /**
     * Create a new node with the specified context.
     *
     * @param context  file and line number for the node
     */
    public FENode(FEContext context)
    {
        this.context = context;
    }
    
    /**
     * Returns the context associated with this node.
     *
     * @return context object with file and line number
     */
    public FEContext getContext()
    {
        return context;
    }

    /**
     * Calls an appropriate method in a visitor object with this as
     * a parameter.
     *
     * @param v  visitor object
     * @return   the value returned by the method corresponding to
     *           this type in the visitor object
     */
    abstract public Object accept(FEVisitor v);
}
