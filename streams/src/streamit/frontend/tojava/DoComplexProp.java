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

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;
import streamit.frontend.passes.SymbolTableVisitor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.ArrayList;

/**
 * Perform constant propagation on function bodies.  This class does
 * all of the work required to convert complex expressions to real
 * ones in front-end code.  It builds up symbol tables for variables,
 * and then runs VarToComplex to replace references to complex
 * variables with complex expressions referencing the fields of the
 * variables.  ComplexProp is then run to cause an expression to be
 * either purely real or be an ExprComplex at the top level.  Finally,
 * this pass inserts statements as necessary to cause all statements
 * to deal with purely real values.
 *
 * Things this is known to punt on:
 * -- Complex parameters to function calls not handled directly
 *    by ComplexProp need to be temporary variables.
 * -- Initialized complex variables should have their value separated
 *    out.
 * -- Semantics of for loops (for(complex c = 1+1i; abs(c) < 5; c += 1i))
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: DoComplexProp.java,v 1.25 2004-07-08 05:45:42 thies Exp $
 */
public class DoComplexProp extends SymbolTableVisitor
{
    private ComplexProp cplxProp;
    private TempVarGen varGen;
    
    public DoComplexProp(TempVarGen varGen)
    {
        super(null, null);
        cplxProp = new ComplexProp();
        this.varGen = varGen;
    }

    private Expression doExprProp(Expression expr)
    {
        expr = (Expression)expr.accept(this);
        expr = (Expression)expr.accept(cplxProp);
        return expr;
    }

    /**
     * Create an initialized temporary variable to handle a
     * complex expression.  If expr is an ExprComplex, uses addStatement()
     * to add statements to declare a new temporary variable and
     * separately initialize its real and complex parts, and return
     * an ExprVar corresponding to the temporary.  Otherwise, return
     * expr.
     */
    private Expression makeComplexTemporary(Expression expr)
    {
        if (!(expr instanceof ExprComplex))
            return expr;
        ExprComplex cplx = (ExprComplex)expr;
        // Check: do we have (c.real)+i(c.imag)?  If so, just return c.
        if (cplx.getReal() instanceof ExprField &&
            cplx.getImag() instanceof ExprField)
        {
            ExprField fr = (ExprField)cplx.getReal();
            ExprField fi = (ExprField)cplx.getImag();
            if (fr.getName().equals("real") && fr.getLeft().isLValue() &&
                fi.getName().equals("imag") && fi.getLeft().isLValue() &&
                fr.getLeft().equals(fi.getLeft()))
            {
                return fi.getLeft();
            }
        }
        // This code path almost certainly isn't going to follow
        // the path tojava.TempVarGen was written for, so we can
        // ignore the type parameter.
        String tempVar = varGen.nextVar();
        Expression exprVar = new ExprVar(expr.getContext(), tempVar);
        Type type = new TypePrimitive(TypePrimitive.TYPE_COMPLEX);
        addStatement(new StmtVarDecl(expr.getContext(), type, tempVar, null));
        symtab.registerVar(tempVar, type);
        addStatement(new StmtAssign(expr.getContext(),
                                    new ExprField(expr.getContext(),
                                                  exprVar, "real"),
                                    cplx.getRealExpr()));
        addStatement(new StmtAssign(expr.getContext(),
                                    new ExprField(expr.getContext(),
                                                  exprVar, "imag"),
                                    cplx.getImagExpr()));
        return exprVar;
    }

    /**
     * Create an initialized temporary variable to hold an arbitrary
     * expression.  Uses addStatement() to add an initializer for
     * the variable, and returns the variable.
     */
    private Expression makeAnyTemporary(Expression expr)
    {
        String tempVar = varGen.nextVar();
        Expression exprVar = new ExprVar(expr.getContext(), tempVar);
        Type type = getType(expr);
        addStatement(new StmtVarDecl(expr.getContext(), type, tempVar, expr));
        symtab.registerVar(tempVar, type);
        return exprVar;
    }

    /**
     * Given an Expression, return a new Expression that is a
     * ExprComplex explicitly referencing its real and imaginary parts.
     */
    private static Expression makeComplexPair(Expression exp)
    {
        Expression real = new ExprField(exp.getContext(), exp, "real");
        Expression imag = new ExprField(exp.getContext(), exp, "imag");
        return new ExprComplex(exp.getContext(), real, imag);
    }

    /**
     * Given a List of Expression, return a new List that has all
     * of the Expressions in l, except with any ExprComplex values
     * replaced with temporary variables.
     */
    private List createListTemporaries(List l)
    {
        List nl = new ArrayList();
        for (Iterator iter = l.iterator(); iter.hasNext(); )
        {
            Expression expr = (Expression)iter.next();
            if (expr instanceof ExprComplex)
                expr = makeComplexTemporary(expr);
            nl.add(expr);
        }
        return nl;
    }

    protected Expression doExpression(Expression expr)
    {
        return doExprProp(expr);
    }

    public Object visitExprPeek(ExprPeek expr)
    {
        Expression x = (Expression)super.visitExprPeek(expr);
        // If the stream's input type is complex, we want a temporary
        // instead.
        if (streamType.getIn().isComplex())
            x = makeAnyTemporary(x);
        return x;
    }

    public Object visitExprPop(ExprPop expr)
    {
        Expression x = (Expression)super.visitExprPop(expr);
        // If the stream's input type is complex, we want a temporary
        // instead.
        // if (streamType.getIn().isComplex())
        //     x = makeAnyTemporary(x);
        return x;
    }

    public Object visitExprVar(ExprVar exp)
    {
        if (getType(exp).isComplex())
            return makeComplexPair(exp);
        else
            return exp;
    }

    public Object visitExprField(ExprField exp)
    {
        // If the expression is already visiting a field of a Complex
        // object, don't recurse further.
        if (getType(exp.getLeft()).isComplex())
            return exp;
        // Perhaps this field is complex.
        if (getType(exp)!=null&&getType(exp).isComplex())
            return makeComplexPair(exp);
        // Otherwise recurse normally.
        return super.visitExprField(exp);
    }

    public Object visitExprArray(ExprArray exp)
    {
        // If the type of the expression is complex, decompose it;
        // otherwise, move on.
        if (getType(exp).isComplex())
	    // this path will never be taken because <exp> will be an
	    // array type, not a complex type, right?  --BFT
            return makeComplexPair(exp);
        else
            return exp;
    }

    public Object visitExprArrayInit(ExprArrayInit exp)
    {
	// Not sure what to do here -- let's try visiting the super.
	// I bet that static initializers with complex numbers would
	// fail at this point.  --BFT
	return super.visitExprArrayInit(exp);
    }

    public Object visitSCSimple(SCSimple creator)
    {
        // Run propagation, but insert temporaries for any
        // complex variables that are left.
        creator = (SCSimple)super.visitSCSimple(creator);
        return new SCSimple(creator.getContext(), creator.getName(),
                            creator.getTypes(),
                            createListTemporaries(creator.getParams()),
                            creator.getPortals());
    }

    public Object visitStmtAssign(StmtAssign stmt)
    {
        Expression lhs = stmt.getLHS();
        Expression rhs = doExprProp(stmt.getRHS());
        if (rhs instanceof ExprComplex)
        {
            ExprComplex cplx = (ExprComplex)rhs;
            addStatement(new StmtAssign(stmt.getContext(),
                                        new ExprField(lhs.getContext(),
                                                      lhs, "real"),
                                        cplx.getRealExpr(),
                                        stmt.getOp()));
            addStatement(new StmtAssign(stmt.getContext(),
                                        new ExprField(lhs.getContext(),
                                                      lhs, "imag"),
                                        cplx.getImagExpr(),
                                        stmt.getOp()));
            return null;
        }
        else if (getType(lhs).isComplex() && !(getType(rhs).isComplex()))
        {
            addStatement(new StmtAssign(stmt.getContext(),
                                        new ExprField(lhs.getContext(),
                                                     lhs, "real"),
                                        rhs,
                                        stmt.getOp()));
            addStatement(new StmtAssign(stmt.getContext(),
                                        new ExprField(lhs.getContext(),
                                                      lhs, "imag"),
                                        new ExprConstInt(lhs.getContext(),
                                                         0),
                                        stmt.getOp()));
            return null;
        }
        else if (rhs != stmt.getRHS())
            return new StmtAssign(stmt.getContext(), lhs, rhs, stmt.getOp());
        else
            return stmt;
    }

    public Object visitStmtEnqueue(StmtEnqueue stmt)
    {
        Expression value = stmt.getValue();
        value = doExprProp(value);
        value = makeComplexTemporary(value);
        return new StmtEnqueue(stmt.getContext(), value);
    }

    public Object visitStmtExpr(StmtExpr stmt)
    {
        Expression newExpr = doExprProp(stmt.getExpression());
        if (newExpr instanceof ExprComplex)
        {
            ExprComplex cplx = (ExprComplex)newExpr;
            addStatement(new StmtExpr(stmt.getContext(), cplx.getRealExpr()));
            addStatement(new StmtExpr(stmt.getContext(), cplx.getImagExpr()));
            return null;
        }
        if (newExpr == stmt.getExpression())
            return stmt;
        return new StmtExpr(stmt.getContext(), newExpr);
    }

    public Object visitStmtPush(StmtPush stmt)
    {
        Expression value = stmt.getValue();
        value = doExprProp(value);
        value = makeComplexTemporary(value);
        return new StmtPush(stmt.getContext(), value);
    }

    public Object visitStmtReturn(StmtReturn stmt)
    {
        Expression value = stmt.getValue();
        if (value == null) return stmt;
        value = doExprProp(value);
        value = makeComplexTemporary(value);
        return new StmtReturn(stmt.getContext(), value);
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        stmt = (StmtVarDecl)super.visitStmtVarDecl(stmt);

        // Save the context, we'll need it later.
        FEContext ctx = stmt.getContext();
        // Go ahead and do propagation:
        List newTypes = new java.util.ArrayList();
        List newNames = new java.util.ArrayList();
        List newInits = new java.util.ArrayList();
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            String name = stmt.getName(i);
            Type type = stmt.getType(i);
            Expression init = stmt.getInit(i);

            // If this is uninitialized, or the type isn't complex,
            // go on with our lives.
            //
            // (But what about things like float foo=abs(a+bi)?  --dzm)
            if (init == null || !type.isComplex())
            {
                newTypes.add(type);
                newNames.add(name);
                newInits.add(init);
                continue;
            }

            // Is the right-hand side complex too?
            Expression exprVar = new ExprVar(ctx, name);
            if (init instanceof ExprComplex)
            {
                // Right.  Create the separate initialization statements.
                ExprComplex cplx = (ExprComplex)init;
                addStatement(new StmtVarDecl(ctx, type, name, null));
                addStatement(new StmtAssign(ctx,
                                            new ExprField(ctx,
                                                          exprVar, "real"),
                                            cplx.getRealExpr()));
                addStatement(new StmtAssign(ctx,
                                            new ExprField(ctx,
                                                          exprVar, "imag"),
                                            cplx.getImagExpr()));
                continue;
            }
            // Maybe the right-hand side isn't complex at all.
            if (!(getType(init).isComplex()))
            {
                addStatement(new StmtVarDecl(ctx, type, name, null));
                addStatement(new StmtAssign(ctx,
                                            new ExprField(ctx,
                                                          exprVar, "real"),
                                            init));
                addStatement(new StmtAssign(ctx,
                                            new ExprField(ctx,
                                                          exprVar, "imag"),
                                            new ExprConstInt(ctx, 0)));
                continue;
            }
            // Otherwise, we have complex foo = (complex)bar(), which is fine.
            newTypes.add(type);
            newNames.add(name);
            newInits.add(init);
        }
        // It's possible that this will leave us with no variables.
        // If so, don't return anything.  Assume all three lists are
        // the same length.
        if (newTypes.isEmpty())
            return null;
        return new StmtVarDecl(ctx, newTypes, newNames, newInits);
    }
}
