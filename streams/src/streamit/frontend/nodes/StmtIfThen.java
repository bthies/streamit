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
 * A standard conditional statement.  This has a conditional expression
 * and two optional statements.  If the condition is true, the first
 * statement (the consequent) is executed; otherwise, the second statement
 * (the alternative) is executed.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StmtIfThen.java,v 1.3 2003-10-09 19:51:00 dmaze Exp $
 */
public class StmtIfThen extends Statement
{
    private Expression cond;
    private Statement cons, alt;
    
    /** Create a new conditional statement, with the specified
     * condition, consequent, and alternative.  The two statements
     * may be null if omitted. */
    public StmtIfThen(FEContext context, Expression cond,
                      Statement cons, Statement alt)
    {
        super(context);
        this.cond = cond;
        this.cons = cons;
        this.alt = alt;
    }
    
    /** Returns the condition of this. */
    public Expression getCond()
    {
        return cond;
    }
    
    /** Returns the consequent statement of this, which is executed if
     * the condition is true. */
    public Statement getCons()
    {
        return cons;
    }
    
    /** Return the alternative statement of this, which is executed if
     * the condition is false. */
    public Statement getAlt()
    {
        return alt;
    }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtIfThen(this);
    }
}
