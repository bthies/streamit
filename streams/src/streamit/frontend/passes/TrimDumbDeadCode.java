package streamit.frontend.passes;

import streamit.frontend.nodes.*;

/**
 * Remove expression statements with no side effects.  In particular,
 * drop expression statements that are purely constants or local
 * variable declarations.  (Don't trim anything with a child
 * expression, that requires effort.)
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TrimDumbDeadCode.java,v 1.2 2003-07-01 15:13:01 dmaze Exp $
 */
public class TrimDumbDeadCode extends FEReplacer
{
    public Object visitStmtExpr(StmtExpr stmt)
    {
        Expression expr = stmt.getExpression();
        // NB: for array and field expressions, we really should look
        // for side-effect-causing children.  At this point in the
        // process it's probably okay, though.
        if (expr instanceof ExprArray ||
            expr instanceof ExprConstBoolean ||
            expr instanceof ExprConstChar ||
            expr instanceof ExprConstFloat ||
            expr instanceof ExprConstInt ||
            expr instanceof ExprConstStr ||
            expr instanceof ExprField ||
            expr instanceof ExprVar)
            return null;
        return stmt;
    }
}
