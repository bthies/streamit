/*
 * StmtIfThen.java: a conditional statement
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtIfThen.java,v 1.2 2002-09-06 16:28:43 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A standard conditional statement.  This has a conditional expression
 * and two optional statements.  If the condition is true, the first
 * statement (the consequent) is executed; otherwise, the second statement
 * (the alternative) is executed.
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
