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
 * Indicates a range such as [min,ave,max] as used in a dynamic rate.
 * Ranges are over integers.
 *
 */
public class ExprRange extends Expression
{
    private Expression min,ave,max;

    /** Create a new ExprDynamicToken. */
    public ExprRange(FEContext context, Expression min, Expression ave, Expression max)
    {
        super(context);
        this.min = min;
        // a null average turns into a dynamic average
        if (ave==null) {
            this.ave = new ExprDynamicToken(context);
        } else {
            this.ave = ave;
        }
        this.max = max;
    }
    
    public ExprRange(FEContext context, Expression min, Expression max)
    {
        this(context, min, null, max);
    }

    /** Create a new ExprRange with no context. */
    public ExprRange(Expression min, Expression ave, Expression max)
    {
        this(null, min, ave, max);
    }

    /** Create a new ExprRange with no context. */
    public ExprRange(Expression min, Expression max) {
        this((FEContext)null, min, max);
    }
    
    /** Return minimum of range. */
    public Expression getMin() {
        return min;
    }

    /** Return average of range. */
    public Expression getAve() {
        return ave;
    }

    /** Return maximum of range. */
    public Expression getMax() {
        return max;
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprRange(this);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof ExprRange))
            return false;
        ExprRange range = (ExprRange)other;
        if (!(min.equals(range.min)))
            return false;
        if (!(ave.equals(range.ave)))
            return false;
        if (!(max.equals(range.max)))
            return false;
        return true;
    }
    
    public int hashCode()
    {
        // following the pattern in the integer constants -- constants
        // of the same value have the same hashcode
        return min.hashCode() * ave.hashCode() * max.hashCode();
    }
    
    public String toString()
    {
        return "[" + min + "," + ave + "," + max + "]";
    }
}
