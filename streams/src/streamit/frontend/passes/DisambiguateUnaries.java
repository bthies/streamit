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
 * @version $Id: DisambiguateUnaries.java,v 1.12 2006-01-25 17:04:28 thies Exp $
 */
public class DisambiguateUnaries extends SymbolTableVisitor
{
    private TempVarGen varGen;
    private List successors;
    /**
     * Whether or not we should disambiguate pops/peeks on the current
     * descent.
     */
    private boolean visitPopPeek;
    
    public DisambiguateUnaries(TempVarGen varGen)
    {
        super(null);
        this.varGen = varGen;
    }
    
    protected void doStatement(Statement stmt)
    {
        successors = new java.util.ArrayList();
        visitPopPeek = calcVisitPopPeek(stmt);
        Statement result = (Statement)stmt.accept(this);
        if (result != null)
            addStatement(result);
        addStatements(successors);

        /* RMR { clear the list of successor statements once they are
         * added to avoid adding them again in the case of a statement
         * which triggers recursive processing (namely an <add> of an
         * anonymous filter); otherwise if the last statement of an
         * anonymous filter results in a non-null list of successor
         * statements (e.g, add filter ... { push(i++); or i++; } the
         * successor statements are added following the <push> as well
         * as following the <add> in the parent stream's initialization 
         * code
         */
        successors.clear();
        /* } RMR */
    }

    /**
     * Determines whether or not we need to replace pops and peeks for
     * a given statement.  We should replace them if either of the
     * following conditions are met:
     *
     *  1) there is more than one pop
     *  2) there is at least one pop and at least one peek
     *
     * These are the cases where the pop has to be well-ordered with
     * the other pops or peeks.
     */
    private boolean calcVisitPopPeek(Statement stmt) {
        final int[] popCount = { 0 };
        final int[] peekCount = { 0 };
        stmt.accept(new FEReplacer() {
                public Object visitExprPop(ExprPop expr) {
                    popCount[0]++;
                    return super.visitExprPop(expr);
                }
                public Object visitExprPeek(ExprPeek expr) {
                    peekCount[0]++;
                    return super.visitExprPeek(expr);
                }
            });
        return (// more than one pop, or
                popCount[0] > 1 ||  
                // at least one pop and at least one peek
                popCount[0] >= 1 && peekCount[0] >= 1);
    }

    public Object visitExprUnary(ExprUnary expr)
    {
        /* RMR { do not transform ++/-- operators to new expressions,
         * instead simply return the original expression
         */
        //         // Does this modify its argument?
        //         int op = expr.getOp();
        //         if (op == ExprUnary.UNOP_PREINC || op == ExprUnary.UNOP_PREDEC ||
        //             op == ExprUnary.UNOP_POSTINC || op == ExprUnary.UNOP_POSTDEC)
        //         {
        //             // Insert a statement: a = a + 1.
        //             // Assume that the child expression of expr is a valid
        //             // left-hand side; it can usefully be a field, array
        //             // reference, or local variable.
        //             FEContext ctx = expr.getContext();
        //             Expression lhs = expr.getExpr();
        //             int bop = ExprBinary.BINOP_ADD;
        //             if (op == ExprUnary.UNOP_PREDEC || op == ExprUnary.UNOP_POSTDEC)
        //                 bop = ExprBinary.BINOP_SUB;
        //             Expression rhs =
        //                 new ExprBinary(ctx, bop, lhs, new ExprConstInt(ctx, 1));
        //             Statement assign = new StmtAssign(ctx, lhs, rhs, 0);
        //             if (op == ExprUnary.UNOP_PREINC || op == ExprUnary.UNOP_PREDEC)
        //                 addStatement(assign);
        //             else
        //                 successors.add(assign);
        //             return lhs;
        //         }
        /* } RMR */
        return expr;
    }

    /**
     * Helper function that visits an arbitrary expression,
     * creates a helper variable containing its value, and returning
     * the helper.
     */
    private Object visitPeekOrPop(Expression expr)
    {
        /* RMR { make sure peek arguements get visited
         * to handle pre/post decrements/increments 
         */
        if (expr instanceof ExprPeek) {
            ExprPeek peekArgument = (ExprPeek) expr;
            expr = new ExprPeek(expr.getContext(), 
                                (Expression) peekArgument.getExpr().accept(this));
        }
        /* } RMR */
      
        // don't change anything if we're not to be visiting pops or
        // peeks
        if (!visitPopPeek) {
            return expr;
        }
      
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
        Statement newBody = (Statement)stmt.getBody().accept(this);
        successors = new java.util.ArrayList();
        Statement newInit = (Statement)stmt.getInit().accept(this);
        if (newInit == stmt.getInit() && newBody == stmt.getBody())
            return stmt;
        return new StmtFor(stmt.getContext(), newInit, stmt.getCond(),
                           stmt.getIncr(), newBody);
    }

    public Object visitStmtIfThen(StmtIfThen stmt)
    {
        // Need to reset successors list in between visiting children.
        Statement newCons = (Statement)stmt.getCons().accept(this);
        successors = new java.util.ArrayList();
        Statement newAlt = stmt.getAlt();
        if (newAlt != null) newAlt = (Statement)newAlt.accept(this);
        successors = new java.util.ArrayList();
        Expression newCond = (Expression)stmt.getCond().accept(this);
        if (newCons == stmt.getCons() &&
            newAlt == stmt.getAlt() &&
            newCond == stmt.getCond())
            return stmt;
        return new StmtIfThen(stmt.getContext(), newCond, newCons, newAlt);
    }

    public Object visitStmtWhile(StmtWhile stmt)
    {
        // Similar problem: if the condition results in inserting
        // statements, they'd need to go both before the loop and
        // at the end of the loop body, and continue statements
        // would go in the wrong place.
        Statement newBody = (Statement)stmt.getBody().accept(this);
        successors = new java.util.ArrayList();
        if (newBody == stmt.getBody())
            return stmt;
        return new StmtWhile(stmt.getContext(), stmt.getCond(), newBody);
    }
}
