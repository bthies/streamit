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
 * A single-character literal, as appears inside single quotes in
 * Java.  This generally doesn't actually appear in StreamIt code.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ExprConstChar.java,v 1.3 2003-10-09 19:50:59 dmaze Exp $
 */
public class ExprConstChar extends Expression
{
    private char val;
    
    /** Create a new ExprConstChar for a particular character. */
    public ExprConstChar(FEContext context, char val)
    {
        super(context);
        this.val = val;
    }
    
    /** Create a new ExprConstChar containing the first character of a
     * String. */
    public ExprConstChar(FEContext context, String str)
    {
        this(context, str.charAt(0));
    }
    
    /** Returns the value of this. */
    public char getVal() { return val; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprConstChar(this);
    }
}
