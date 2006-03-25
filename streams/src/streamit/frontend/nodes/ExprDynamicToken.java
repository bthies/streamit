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
 * Indicates the '*' token as used in a dynamic rate declaration.  Its
 * type is an integer.
 *
 */
public class ExprDynamicToken extends Expression
{
    /** Create a new ExprDynamicToken. */
    public ExprDynamicToken(FEContext context)
    {
        super(context);
    }

    /** Create a new ExprDynamicToken with no context. */
    public ExprDynamicToken()
    {
        this(null);
    }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprDynamicToken(this);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof ExprDynamicToken))
            return false;
        return true;
    }
    
    public int hashCode()
    {
        // following the pattern in the integer constants -- constants
        // of the same value have the same hashcode
        return new Character('*').hashCode();
    }
    
    public String toString()
    {
        return "*";
    }
}
