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
 * A boolean-valued constant.  This can be freely promoted to any
 * other type; if converted to a real numeric type, "true" has value
 * 1, "false" has value 0.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ExprConstBoolean.java,v 1.2 2003-10-09 19:50:59 dmaze Exp $
 */
public class ExprConstBoolean extends Expression
{
    private boolean val;
    
    /**
     * Create a new ExprConstBoolean with a specified value.
     *
     * @param context  Context indicating file and line number this
     *                 constant was created in
     * @param val      Value of the constant
     */
    public ExprConstBoolean(FEContext context, boolean val)
    {
        super(context);
        this.val = val;
    }

    /**
     * Create a new ExprConstBoolean with a specified value but no
     * context.
     *
     * @param val  Value of the constant
     */
    public ExprConstBoolean(boolean val)
    {
        this(null, val);
    }
    
    /** Returns the value of this. */
    public boolean getVal() { return val; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprConstBoolean(this);
    }
}
