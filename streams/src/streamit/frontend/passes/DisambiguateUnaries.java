package streamit.frontend.passes;

import streamit.frontend.nodes.*;
import java.util.List;

/**
 * Give a rigid ordering to operations such as ++, --, and pop().
 * Do this by a post-order depth-first traversal of expression trees;
 * if we see a unary increment or decrement or a pop() or peek()
 * operation, move it into a separate statement and replace it with
 * a temporary variable.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: DisambiguateUnaries.java,v 1.4 2003-07-30 20:31:52 dmaze Exp $
 */
public class DisambiguateUnaries extends SymbolTableVisitor
{
    private TempVarGen varGen;
    private List successors;
    
    public DisambiguateUnaries(TempVarGen varGen)
    {
        super(null);
        this.varGen = varGen;
    }
    
    protected void doStatement(Statement stmt)
    {
        successors = new java.util.ArrayList();
        Statement result = (Statement)stmt.accept(this);
        if (result != null)
            addStatement(result);
        addStatements(successors);
    }

    public Object visitExprUnary(ExprUnary expr)
    {
        // Does this modify its argument?
        int op = expr.getOp();
        if (op == ExprUnary.UNOP_PREINC || op == ExprUnary.UNOP_PREDEC ||
            op == ExprUnary.UNOP_POSTINC || op == ExprUnary.UNOP_POSTDEC)
        {
            // Insert a statement: a = a + 1.
            // Assume that the child expression of expr is a valid
            // left-hand side; it can usefully be a field, array
            // reference, or local variable.
            FEContext ctx = expr.getContext();
            Expression lhs = expr.getExpr();
            int bop = ExprBinary.BINOP_ADD;
            if (op == ExprUnary.UNOP_PREDEC || op == ExprUnary.UNOP_POSTDEC)
                bop = ExprBinary.BINOP_SUB;
            Expression rhs =
                new ExprBinary(ctx, bop, lhs, new ExprConstInt(ctx, 1));
            Statement assign = new StmtAssign(ctx, lhs, rhs, 0);
            if (op == ExprUnary.UNOP_PREINC || op == ExprUnary.UNOP_PREDEC)
                addStatement(assign);
            else
                successors.add(assign);
            return lhs;
        }
        return expr;
    }

    /**
     * Helper function that visits an arbitrary expression,
     * creates a helper variable containing its value, and returning
     * the helper.
     */
    private Object visitPeekOrPop(Expression expr)
    {
        // Create a temporary variable...
        FEContext ctx = expr.getContext();
        String name = varGen.nextVar();
        Type type = getType(expr);
        addStatement(new StmtVarDecl(ctx, type, name, null));
        // Generate an assignment to that..
        Expression var = new ExprVar(ctx, name);
        addStatement(new StmtAssign(ctx, var, expr, 0));
        // ...and return the variable.
        return var;
    }

    public Object visitExprPeek(ExprPeek expr)
    {
        // Why do we need to visit peek expressions here?  If we have
        // peek(0) + pop() + peek(0), the two peeks have different
        // values, since the pop() shifts the input tape by one.
        // If we only moved pops, both peeks would refer to peek(1),
        // which is wrong.
        return visitPeekOrPop(expr);
    }

    public Object visitExprPop(ExprPop expr)
    {
        return visitPeekOrPop(expr);
    }

    public Object visitStmtFor(StmtFor stmt)
    {
        // C-style for loops are a *big pain*: if nothing else, the
        // possible presence of a continue statement means that the
        // increment statement can't be moved inside the loop.
        // Don't visit the condition or increment statement for
        // this reason.  Do visit the init statement (any code it
        // adds gets put before the loop, which is fine) and the
        // body (which should always be a StmtBlock).
        Statement newInit = (Statement)stmt.getInit().accept(this);
        Statement newBody = (Statement)stmt.getBody().accept(this);
        if (newInit == stmt.getInit() && newBody == stmt.getBody())
            return stmt;
        return new StmtFor(stmt.getContext(), newInit, stmt.getCond(),
                           stmt.getIncr(), newBody);
    }

    public Object visitStmtWhile(StmtWhile stmt)
    {
        // Similar problem: if the condition results in inserting
        // statements, they'd need to go both before the loop and
        // at the end of the loop body, and continue statements
        // would go in the wrong place.
        Statement newBody = (Statement)stmt.getBody().accept(this);
        if (newBody == stmt.getBody())
            return stmt;
        return new StmtWhile(stmt.getContext(), stmt.getCond(), newBody);
    }
}
