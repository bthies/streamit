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
 * A return statement with an optional value.  Functions returning
 * void (including init and work functions and message handlers)
 * should have return statements with no value; helper functions
 * returning a particular type should have return statements with
 * expressions of that type.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StmtReturn.java,v 1.3 2003-10-09 19:51:00 dmaze Exp $
 */
public class StmtReturn extends Statement
{
    Expression value;
    
    /** Creates a new return statement, with the specified return value
     * (or null). */
    public StmtReturn(FEContext context, Expression value)
    {
        super(context);
        this.value = value;
    }

    /** Returns the return value of this, or null if there is no return
     * value. */
    public Expression getValue()
    {
        return value;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtReturn(this);
    }
}
