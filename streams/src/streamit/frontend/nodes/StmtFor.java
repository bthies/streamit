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
 * A C-style for loop.  The loop contains an initialization statement,
 * a loop condition, and an increment statement.  On entry, the
 * initialization statement is executed, and then the condition is
 * evaluated.  If the condition is true, the body is executed,
 * followed by the increment statement.  This loops until the
 * condition returns false.  Continue statements cause control flow to
 * go to the increment statement.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StmtFor.java,v 1.3 2003-10-09 19:51:00 dmaze Exp $
 */
public class StmtFor extends Statement
{
    private Expression cond;
    private Statement init, incr, body;
    
    /** Creates a new for loop. */
    public StmtFor(FEContext context, Statement init, Expression cond,
                   Statement incr, Statement body)
    {
        super(context);
        this.init = init;
        this.cond = cond;
        this.incr = incr;
        this.body = body;
    }
    
    /** Return the initialization statement of this. */
    public Statement getInit()
    {
        return init;
    }
    
    /** Return the loop condition of this. */
    public Expression getCond()
    {
        return cond;
    }
    
    /** Return the increment statement of this. */
    public Statement getIncr()
    {
        return incr;
    }
    
    /** Return the loop body of this. */
    public Statement getBody()
    {
        return body;
    }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtFor(this);
    }
}

