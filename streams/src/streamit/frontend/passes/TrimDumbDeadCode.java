package streamit.frontend.passes;

import streamit.frontend.nodes.*;

/**
 * Remove expression statements with no side effects.  In particular,
 * drop expression statements that are purely constants or local
 * variable declarations.  (Don't trim anything with a child
 * expression, that requires effort.)
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TrimDumbDeadCode.java,v 1.1 2003-05-20 18:37:39 dmaze Exp $
 */
public class TrimDumbDeadCode extends FEReplacer
{
    public Object visitStmtExpr(StmtExpr stmt)
    {
        Expression expr = stmt.getExpression();
        if (expr instanceof ExprConstBoolean ||
            expr instanceof ExprConstChar ||
            expr instanceof ExprConstFloat ||
            expr instanceof ExprConstInt ||
            expr instanceof ExprConstStr ||
            expr instanceof ExprVar)
            return null;
        return stmt;
    }
}
