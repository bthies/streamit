/*
 * StmtContinue.java: a simple continue statement
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtContinue.java,v 1.1 2002-09-04 15:12:57 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * StmtContinue is a simple continue statement.  It jumps to evaluating
 * the condition if the innermost loop is a (do/)while loop, or to the
 * increment statement if the innermost loop is a for loop.
 */
public class StmtContinue extends Statement
{
    /** Creates a new continue statement. */
    public StmtContinue(FEContext context)
    {
        super(context);
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        // return v.visitStmtContinue(this);
        return null;
    }
}

