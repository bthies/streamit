/*
 * FEReplacer.java: run through a front-end tree and replace nodes
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: FEReplacer.java,v 1.3 2002-07-11 20:58:22 dmaze Exp $
 */

package streamit.frontend.nodes;

import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * Replace nodes in a front-end tree.  This is a skeleton for writing
 * replacing passes, which implements FEVisitor.  On its own it does nothing,
 * but it is a convenice class for deriving your own replacers from.  All
 * of the member functions of FEReplacer return Expressions; an attempt is
 * made to not create new objects if they would be identical to the
 * original objects.
 */
public class FEReplacer implements FEVisitor
{
    public Object visitExprArray(ExprArray exp)
    {
        Expression base = (Expression)exp.getBase().accept(this);
        Expression offset = (Expression)exp.getOffset().accept(this);
        if (base == exp.getBase() && offset == exp.getOffset())
            return exp;
        else
            return new ExprArray(base, offset);
    }
    
    public Object visitExprBinary(ExprBinary exp)
    {
        Expression left = (Expression)exp.getLeft().accept(this);
        Expression right = (Expression)exp.getRight().accept(this);
        if (left == exp.getLeft() && right == exp.getRight())
            return exp;
        else
            return new ExprBinary(exp.getOp(), left, right);
    }
    
    public Object visitExprComplex(ExprComplex exp)
    {
        Expression real = exp.getReal();
        if (real != null) real = (Expression)real.accept(this);
        Expression imag = exp.getImag();
        if (imag != null) imag = (Expression)imag.accept(this);
        if (real == exp.getReal() && imag == exp.getImag())
            return exp;
        else
            return new ExprComplex(real, imag);
    }
    
    public Object visitExprConstChar(ExprConstChar exp) { return exp; }
    public Object visitExprConstFloat(ExprConstFloat exp) { return exp; }
    public Object visitExprConstInt(ExprConstInt exp) { return exp; }
    public Object visitExprConstStr(ExprConstStr exp) { return exp; }

    public Object visitExprField(ExprField exp)
    {
        Expression left = (Expression)exp.getLeft().accept(this);
        if (left == exp.getLeft())
            return exp;
        else
            return new ExprField(left, exp.getName());
    }

    public Object visitExprFunCall(ExprFunCall exp)
    {
        boolean hasChanged = false;
        List newParams = new ArrayList();
        for (Iterator iter = exp.getParams().iterator(); iter.hasNext(); )
        {
            Expression param = (Expression)iter.next();
            Expression newParam = (Expression)param.accept(this);
            newParams.add(newParam);
            if (param != newParam) hasChanged = true;
        }
        if (!hasChanged) return exp;
        return new ExprFunCall(exp.getName(), newParams);
    }

    public Object visitExprPeek(ExprPeek exp)
    {
        Expression expr = (Expression)exp.getExpr().accept(this);
        if (expr == exp.getExpr())
            return exp;
        else
            return new ExprPeek(expr);
    }

    public Object visitExprPop(ExprPop exp) { return exp; }
    
    public Object visitExprTernary(ExprTernary exp)
    {
        Expression a = (Expression)exp.getA().accept(this);
        Expression b = (Expression)exp.getB().accept(this);
        Expression c = (Expression)exp.getC().accept(this);
        if (a == exp.getA() && b == exp.getB() && c == exp.getC())
            return exp;
        else
            return new ExprTernary(exp.getOp(), a, b, c);
    }
    
    public Object visitExprUnary(ExprUnary exp)
    {
        Expression expr = (Expression)exp.getExpr().accept(this);
        if (expr == exp.getExpr())
            return exp;
        else
            return new ExprUnary(exp.getOp(), expr);
    }
    
    public Object visitExprVar(ExprVar exp) { return exp; }
}
