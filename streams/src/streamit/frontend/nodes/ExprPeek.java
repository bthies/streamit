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
 * A StreamIt peek expression.  This returns a single item off of the current
 * filter's input tape, without removing it; its type is the input type of
 * the filter.  This expression has a single child expression indicating
 * which item off the tape is to be read.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ExprPeek.java,v 1.4 2004-01-16 20:46:23 dmaze Exp $
 */
public class ExprPeek extends Expression
{
    private Expression expr;
    
    /**
     * Creates a new peek expression.
     *
     * @param context  file and line number of the expression
     * @param expr     position on the tape to peek at
     */
    public ExprPeek(FEContext context, Expression expr)
    {
        super(context);
        this.expr = expr;
    }

    /**
     * Returns the position on the tape to be read.
     *
     * @return expression indicating which item is to be read
     */
    public Expression getExpr()
    {
        return expr;
    }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprPeek(this);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof ExprPeek))
            return false;
        ExprPeek ep = (ExprPeek)other;
        if (!(expr.equals(ep.expr)))
            return false;
        return true;
    }

    public int hashCode()
    {
        return expr.hashCode();
    }
    
    public String toString()
    {
        return "peek(" + expr + ")";
    }
}
