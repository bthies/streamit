/*
 * StmtBreak.java: a simple break statement
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtBreak.java,v 1.1 2002-09-04 15:12:56 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * StmtBreak is a simple break statement.  It is used to exit the innermost
 * section of control flow, such as a for or while loop.
 */
public class StmtBreak extends Statement
{
    /** Creates a new break statement. */
    public StmtBreak(FEContext context)
    {
        super(context);
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        // return v.visitStmtBreak(this);
        return null;
    }
}

