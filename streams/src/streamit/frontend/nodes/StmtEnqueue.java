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
 * Push a single value on to the feedback input of the current
 * feedback loop's joiner.  This statement has an expression, which is
 * the value to be enqueued.  The type of the expression must match
 * the output type of the loop stream exactly; this is also the same
 * type as the input type of the feedback loop, unless that type is
 * void.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StmtEnqueue.java,v 1.3 2003-10-09 19:51:00 dmaze Exp $
 */
public class StmtEnqueue extends Statement
{
    Expression value;
    
    /** Creates a new enqueue statement with the specified value. */
    public StmtEnqueue(FEContext context, Expression value)
    {
        super(context);
        this.value = value;
    }

    /** Returns the value this enqueues. */
    public Expression getValue()
    {
        return value;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtEnqueue(this);
    }
}
