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
 * A StreamIt pop expression.  This pops a single item off of the current
 * filter's input tape; its type is the input type of the filter.  This
 * expression has no internal state.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ExprPop.java,v 1.4 2004-01-16 20:42:55 dmaze Exp $
 */
public class ExprPop extends Expression
{
    /**
     * Creates a new pop expression.
     *
     * @param context  file and line number of the expression
     */
    public ExprPop(FEContext context)
    {
        super(context);
    }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprPop(this);
    }

    public boolean equals(Object other)
    {
        // No state; any two pop expressions are equal.
        if (other instanceof ExprPop)
            return true;
        return false;
    }

    public int hashCode()
    {
        // No state, so...
        return 17;
    }
    
    public String toString()
    {
        return "pop()";
    }
}
