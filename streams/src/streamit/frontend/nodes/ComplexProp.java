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

// Does this actually belong here?  If we evolve more front-end passes,
// it can move.  --dzm
package streamit.frontend.nodes;

import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * A pass to propagate complex values upwards in expression trees.
 * This pass looks for nodes with complex-valued children, and replaces
 * them with complex nodes with real-valued children.  In this process,
 * it expands operations such as complex multiply using the correct
 * operations on its children.  This pass allows other code to easily
 * test whether an expression is complex- or real-valued by examining
 * the top-level node, which either is or isn't ExprComplex.  (Not
 * quite true, actually, since the node may reference a complex-valued
 * variable, field, or array element; another pass would need to split
 * these out first.)
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ComplexProp.java,v 1.14 2003-10-09 19:50:59 dmaze Exp $
 */
public class ComplexProp extends FEReplacer
{
    /* By way of implementation: several things here are ignored.
     * Of note:
     * -- ExprArray, ExprField, and ExprVar are ignored; they should
     *    never have complex children, and we need an earlier pass
     *    to disambiguate these.
     * -- Constants are always real.
     */

    /** Helper function to coerce an expression into a complex
     * expression.  If the passed expression is real, return an
     * expression with that expression as the real part and 0.0
     * (not null) as the imaginary part.  If the passed expression
     * is complex, return a complex expression with 0.0 replacing
     * nulls in either part. */
    private static ExprComplex makeComplex(Expression exp)
    {
        if (!(exp instanceof ExprComplex))
            return new ExprComplex(exp.getContext(), exp, new ExprConstFloat(0.0));
        ExprComplex cplx = (ExprComplex)exp;
        if (cplx.getReal() != null && cplx.getImag() != null)
            return cplx;
        Expression real, imag;
        real = cplx.getReal();
        if (real == null) real = new ExprConstFloat(0.0);
        imag = cplx.getImag();
        if (imag == null) imag = new ExprConstFloat(0.0);
        return new ExprComplex(exp.getContext(), real, imag);
    }

    public Object visitExprBinary(ExprBinary exp)
    {
        Expression left = (Expression)exp.getLeft().accept(this);
        Expression right = (Expression)exp.getRight().accept(this);
        FEContext ctx = exp.getContext();
        // We need at least one side to be complex to care.
        if ((left instanceof ExprComplex) || (right instanceof ExprComplex))
        {
            // Okay, build child expressions for the real and imaginary
            // parts of both sides.
            Expression lr, li, rr, ri;
            Expression ar, ai, br, bi, denom;
            if (left instanceof ExprComplex)
            {
                lr = ((ExprComplex)left).getReal();
                li = ((ExprComplex)left).getImag();
            }
            else
            {
                lr = left;
                li = null;
            }
            if (right instanceof ExprComplex)
            {
                rr = ((ExprComplex)right).getReal();
                ri = ((ExprComplex)right).getImag();
            }
            else
            {
                rr = right;
                ri = null;
            }

            // Expressions to be used for returning values:
            Expression real, imag;
            
            // Decide what to do based on the operation:
            switch (exp.getOp())
            {
                // Deal with real and imaginary parts piecewise:
            case ExprBinary.BINOP_ADD:
                if (lr == null)
                    real = rr;
                else if (rr == null)
                    real = lr;
                else
                    real = new ExprBinary(ctx, exp.getOp(), lr, rr);
                
                if (li == null)
                    imag = ri;
                else if (ri == null)
                    imag = li;
                else
                    imag = new ExprBinary(ctx, exp.getOp(), li, ri);
                
                return new ExprComplex(ctx, real, imag);

            case ExprBinary.BINOP_SUB:
                if (lr == null)
                    real = new ExprUnary(ctx, ExprUnary.UNOP_NEG, rr);
                else if (rr == null)
                    real = lr;
                else
                    real = new ExprBinary(ctx, exp.getOp(), lr, rr);
                
                if (li == null)
                    imag = new ExprUnary(ctx, ExprUnary.UNOP_NEG, ri);
                else if (ri == null)
                    imag = li;
                else
                    imag = new ExprBinary(ctx, exp.getOp(), li, ri);
                
                return new ExprComplex(ctx, real, imag);

                // Complex multiply:
            case ExprBinary.BINOP_MUL:
                // (lr + i*li) * (rr + i*ri)
                // lr * rr + i*lr*ri + i*rr*li - li * ri
                // (lr * rr - li * ri) + i*(lr*ri + rr*li)
                // Note that some of these terms can be simplified if
                // various parts are null (always zero).
                // But we won't do that for now; it involves thinking
                // more.
                if (lr == null) lr = new ExprConstFloat(0.0);
                if (li == null) li = new ExprConstFloat(0.0);
                if (rr == null) rr = new ExprConstFloat(0.0);
                if (ri == null) ri = new ExprConstFloat(0.0);
                
                ar = new ExprBinary(ctx, ExprBinary.BINOP_MUL, lr, rr);
                br = new ExprBinary(ctx, ExprBinary.BINOP_MUL, li, ri);
                real = new ExprBinary(ctx, ExprBinary.BINOP_SUB, ar, br);
                
                ai = new ExprBinary(ctx, ExprBinary.BINOP_MUL, lr, ri);
                bi = new ExprBinary(ctx, ExprBinary.BINOP_MUL, li, rr);
                imag = new ExprBinary(ctx, ExprBinary.BINOP_ADD, ai, bi);
                
                return new ExprComplex(ctx, real, imag);
                
                // Complex divide:
            case ExprBinary.BINOP_DIV:
                // This works out to
                // (lr*rr+li*ri)/(rr*rr-ri*ri) + i(li*rr-lr*ri)/(rr*rr-ri*ri)
                // So precalculate the denominator, and share it between
                // both sides.
                // Again, punt on any cleverness involving null bits.
                if (lr == null) lr = new ExprConstFloat(0.0);
                if (li == null) li = new ExprConstFloat(0.0);
                if (rr == null) rr = new ExprConstFloat(0.0);
                if (ri == null) ri = new ExprConstFloat(0.0);

                ar = new ExprBinary(ctx, ExprBinary.BINOP_MUL, rr, rr);
                br = new ExprBinary(ctx, ExprBinary.BINOP_MUL, ri, ri);
                denom = new ExprBinary(ctx, ExprBinary.BINOP_ADD, ar, br);
                
                ar = new ExprBinary(ctx, ExprBinary.BINOP_MUL, lr, rr);
                br = new ExprBinary(ctx, ExprBinary.BINOP_MUL, li, ri);
                real = new ExprBinary(ctx, ExprBinary.BINOP_ADD, ar, br);
                real = new ExprBinary(ctx, ExprBinary.BINOP_DIV, real, denom);
                
                ai = new ExprBinary(ctx, ExprBinary.BINOP_MUL, li, rr);
                bi = new ExprBinary(ctx, ExprBinary.BINOP_MUL, lr, ri);
                imag = new ExprBinary(ctx, ExprBinary.BINOP_SUB, ai, bi);
                imag = new ExprBinary(ctx, ExprBinary.BINOP_DIV, imag, denom);
                
                return new ExprComplex(ctx, real, imag);
                
                // Direct comparisons need to get translated to
                // piecewise comparisons, but the result is a single
                // real-valued expression.
            case ExprBinary.BINOP_EQ:
                // Need to construct (lr == rr) && (li == ri),
                // forcing things to be 0.0 if needed.
                if (lr == null) lr = new ExprConstFloat(0.0);
                if (li == null) li = new ExprConstFloat(0.0);
                if (rr == null) rr = new ExprConstFloat(0.0);
                if (ri == null) ri = new ExprConstFloat(0.0);

                left = new ExprBinary(ctx, ExprBinary.BINOP_EQ, lr, rr);
                right = new ExprBinary(ctx, ExprBinary.BINOP_EQ, li, ri);
                return new ExprBinary(ctx, ExprBinary.BINOP_AND, left, right);

            case ExprBinary.BINOP_NEQ:
                // Need to construct (lr != rr) || (li != ri),
                // forcing things to be 0.0 if needed.
                if (lr == null) lr = new ExprConstFloat(0.0);
                if (li == null) li = new ExprConstFloat(0.0);
                if (rr == null) rr = new ExprConstFloat(0.0);
                if (ri == null) ri = new ExprConstFloat(0.0);

                left = new ExprBinary(ctx, ExprBinary.BINOP_NEQ, lr, rr);
                right = new ExprBinary(ctx, ExprBinary.BINOP_NEQ, li, ri);
                return new ExprBinary(ctx, ExprBinary.BINOP_OR, left, right);
                
                // Anything else is wrong; punt on the problem.
            default:
            }

            // Reconstruct the left and right sides if we need to.
            // For now, always recreate them (we need to test whether
            // left and right are complex).
            // if (lr != left.getReal() || li != left.getImag())
            left = new ExprComplex(ctx, lr, li);
            // if (rr != right.getReal() || ri != right.getImag())
            right = new ExprComplex(ctx, rr, ri);
        }
        // Now build the new result, but only if we need to.
        if (left == exp.getLeft() && right == exp.getRight())
            return exp;
        else
            return new ExprBinary(exp.getContext(), exp.getOp(), left, right);
    }

    public Object visitExprTernary(ExprTernary exp)
    {
        // Only one case of these, so don't bother checking the op.
        Expression a = (Expression)exp.getA().accept(this);
        Expression b = (Expression)exp.getB().accept(this);
        Expression c = (Expression)exp.getC().accept(this);
        FEContext ctx = exp.getContext();

        if (a == exp.getA() && b == exp.getB() && c == exp.getC())
            return exp;

        // Now, we assume that a is real.  If neither b nor c is
        // complex, then the whole thing is real:
        if (!(b instanceof ExprComplex) && !(c instanceof ExprComplex))
            return new ExprTernary(ctx, exp.getOp(), a, b, c);

        // Otherwise, force both b and c to be complex.
        ExprComplex bc = makeComplex(b);
        ExprComplex cc = makeComplex(c);
        
        // Now create the pair of ternary expressions.
        Expression real = new ExprTernary(ctx, exp.getOp(), a,
                                          bc.getReal(), cc.getReal());
        Expression imag = new ExprTernary(ctx, exp.getOp(), a,
                                          bc.getImag(), cc.getImag());
        return new ExprComplex(ctx, real, imag);
    }

    public Object visitExprUnary(ExprUnary exp)
    {
        // The only operation this makes sense for is negation,
        // and we're ignoring error-checking everywhere else anyways.
        // So assume every unary operation is like negation.  In that
        // case, -(a+bi) == (-a)+(-b)i.
        Expression expr = (Expression)exp.getExpr().accept(this);
        int op = exp.getOp();
        if (!(expr instanceof ExprComplex))
            return new ExprUnary(exp.getContext(), op, expr);
        ExprComplex cplx = makeComplex(expr);
        Expression real = new ExprUnary(exp.getContext(), op, cplx.getReal());
        Expression imag = new ExprUnary(exp.getContext(), op, cplx.getImag());
        return new ExprComplex(exp.getContext(), real, imag);
    }

    public Object visitExprFunCall(ExprFunCall exp)
    {
        // Start by resolving all of the parameters.
        List params = new ArrayList();
        Iterator iter = exp.getParams().iterator();
        while (iter.hasNext())
        {
            Expression expr = (Expression)iter.next();
            params.add(expr.accept(this));
        }
        
        // Lots of special cases here.  We care about a function if
        // it matches the conditions in isEligibleFunCall().
        if (isEligibleFunCall(exp, params, "abs", 1))
            return fcAbs(exp, (ExprComplex)params.get(0));
        if (isEligibleFunCall(exp, params, "arg", 1))
            return fcArg(exp, (ExprComplex)params.get(0));
        if (isEligibleFunCall(exp, params, "exp", 1))
            return fcExp(exp, (ExprComplex)params.get(0));
        if (isEligibleFunCall(exp, params, "log", 1))
            return fcLog(exp, (ExprComplex)params.get(0));
        if (isEligibleFunCall(exp, params, "sin", 1))
            return fcSin(exp, (ExprComplex)params.get(0));
        if (isEligibleFunCall(exp, params, "cos", 1))
            return fcCos(exp, (ExprComplex)params.get(0));
	if (isEligibleFunCall(exp, params, "pow", 2))
	    return fcPow(exp, (Expression)params.get(0),
			 (Expression)params.get(1));

        // sqrt() is special; sqrt() of a real can return a complex
        // answer if its argument is negative, but we don't always
        // want sqrt() to return complex.  So have sqrt(complex)
        // return complex, and csqrt(anything) also return complex.
        if (isEligibleFunCall(exp, params, "sqrt", 1))
            return fcSqrt(exp, (ExprComplex)params.get(0));
        if (exp.getName().equals("csqrt"))
            return fcCSqrt(exp, (Expression)params.get(0));
        
        return new ExprFunCall(exp.getContext(), exp.getName(), params);
    }

    private boolean isEligibleFunCall(ExprFunCall exp, List params, String fn,
				      int nParams)
    {
        // A function call is eligible if:
        // -- Its name is exactly fn;
        // -- It has exactly nParams parameters;
        // -- Any parameter is complex.
        if (!(exp.getName().equals(fn)))
            return false;
        if (params.size() != nParams)
            return false;
	for (Iterator iter = params.iterator(); iter.hasNext(); )
	{
	    Expression param = (Expression)iter.next();
	    if (param instanceof ExprComplex)
		return true;
	}
        return false;
    }

    public Expression fcAbs(ExprFunCall fc, ExprComplex param)
    {
        // sqrt(a^2 + b^2) -- always real
        Expression a = param.getReal();
        Expression b = param.getImag();
        Expression a2 = new ExprBinary(a.getContext(), ExprBinary.BINOP_MUL, a, a);
        Expression b2 = new ExprBinary(b.getContext(), ExprBinary.BINOP_MUL, b, b);
        Expression a2b2 = new ExprBinary(a2.getContext(), ExprBinary.BINOP_ADD, a2, b2);
        Expression result = new ExprFunCall(fc.getContext(), "sqrt", a2b2);
        return result;
    }

    public Expression fcArg(ExprFunCall fc, ExprComplex param)
    {
        // atan(b/a) -- always real
        // The C and Java libraries both supply atan2(), which
        // performs this function and gives us the correct angle.
        return new ExprFunCall(fc.getContext(),
                               "atan2", param.getImag(), param.getReal());
    }

    public Expression fcExp(ExprFunCall fc, ExprComplex param)
    {
        // e^(a+bi)
        // e^a e^bi
        // e^a cis b
        Expression eToA = new ExprFunCall(param.getContext(),
                                          "exp", param.getReal());
        Expression cosB = new ExprFunCall(param.getContext(),
                                          "cos", param.getImag());
        Expression sinB = new ExprFunCall(param.getContext(),
                                          "sin", param.getImag());
        
        Expression real = new ExprBinary(eToA.getContext(), ExprBinary.BINOP_MUL, eToA, cosB);
        Expression imag = new ExprBinary(eToA.getContext(), ExprBinary.BINOP_MUL, eToA, sinB);
        return new ExprComplex(fc.getContext(), real, imag);
    }

    public Expression fcLog(ExprFunCall fc, ExprComplex param)
    {
        // log |z| + i arg(z)
        Expression absZ = new ExprFunCall(param.getContext(), "abs", param);
        absZ = (Expression)absZ.accept(this);
        Expression logAbsZ = new ExprFunCall(absZ.getContext(), "log", absZ);
        
        Expression argZ = new ExprFunCall(param.getContext(), "arg", param);
        argZ = (Expression)argZ.accept(this);
        
        return new ExprComplex(fc.getContext(), logAbsZ, argZ);
    }

    public Expression fcSin(ExprFunCall fc, ExprComplex param)
    {
        // (e^(iz)-e^(-iz))/(2i)
        // (e^-b+e^b)/2 sin a + i(e^b-e^-b)/2 cos a
        Expression a = param.getReal();
        Expression b = param.getImag();
        Expression minusB = new ExprUnary(b.getContext(),
                                          ExprUnary.UNOP_NEG, b);
        Expression eB = new ExprFunCall(b.getContext(), "exp", b);
        Expression eMinusB = new ExprFunCall(minusB.getContext(),
                                             "exp", minusB);
        Expression sinA = new ExprFunCall(a.getContext(), "sin", a);
        Expression cosA = new ExprFunCall(a.getContext(), "cos", a);
        
        Expression rNum = new ExprBinary(eB.getContext(),
                                         ExprBinary.BINOP_ADD, eB, eMinusB);
        Expression rMag = new ExprBinary(rNum.getContext(),
                                         ExprBinary.BINOP_DIV, rNum,
                                         new ExprConstInt(2));
        Expression real = new ExprBinary(rMag.getContext(),
                                         ExprBinary.BINOP_MUL, rMag, sinA);
        
        Expression iNum = new ExprBinary(eB.getContext(),
                                         ExprBinary.BINOP_SUB, eB, eMinusB);
        Expression iMag = new ExprBinary(iNum.getContext(),
                                         ExprBinary.BINOP_DIV, iNum,
                                         new ExprConstInt(2));
        Expression imag = new ExprBinary(iMag.getContext(),
                                         ExprBinary.BINOP_MUL, iMag, cosA);
        
        return new ExprComplex(fc.getContext(), real, imag);
    }
    
    public Expression fcCos(ExprFunCall fc, ExprComplex param)
    {
        // (e^(iz)+e^(-iz))/(2)
        // (e^-b+e^b)/2 cos a + i(e^-b-e^b)/2 sin a
        Expression a = param.getReal();
        Expression b = param.getImag();
        Expression minusB = new ExprUnary(b.getContext(), ExprUnary.UNOP_NEG, b);
        Expression eB = new ExprFunCall(b.getContext(), "exp", b);
        Expression eMinusB = new ExprFunCall(minusB.getContext(),
                                             "exp", minusB);
        Expression sinA = new ExprFunCall(a.getContext(), "sin", a);
        Expression cosA = new ExprFunCall(a.getContext(), "cos", a);
        
        Expression rNum = new ExprBinary(eMinusB.getContext(),
                                         ExprBinary.BINOP_ADD, eMinusB, eB);
        Expression rMag = new ExprBinary(rNum.getContext(),
                                         ExprBinary.BINOP_DIV, rNum,
                                         new ExprConstInt(2));
        Expression real = new ExprBinary(rMag.getContext(),
                                         ExprBinary.BINOP_MUL, rMag, cosA);
        
        Expression iNum = new ExprBinary(eMinusB.getContext(),
                                         ExprBinary.BINOP_SUB, eMinusB, eB);
        Expression iMag = new ExprBinary(iNum.getContext(),
                                         ExprBinary.BINOP_DIV, iNum,
                                         new ExprConstInt(2));
        Expression imag = new ExprBinary(iMag.getContext(),
                                         ExprBinary.BINOP_MUL, iMag, sinA);
        
        return new ExprComplex(fc.getContext(), real, imag);
    }

    public Expression fcPow(ExprFunCall fc, Expression base, Expression exp)
    {
	// In general, base^exp = e^(exp*ln(base)).  This is easy enough
	// to do here, solves all of the nasty corner cases, and might
	// have a prayer of being efficient.
	Expression lnBase = new ExprFunCall(base.getContext(), "log", base);
	Expression newExp = new ExprBinary(exp.getContext(),
					   ExprBinary.BINOP_MUL,
					   exp, lnBase);
	Expression result = new ExprFunCall(fc.getContext(), "exp", newExp);
	result = (Expression)result.accept(this);
	return result;
    }

    public Expression fcSqrt(ExprFunCall fc, ExprComplex param)
    {
        // If we convert to polar form here, then one root is
        // sqrt(|z|)cis(arg(z)/2).  It's probably more efficient to do
        // a rectangular calculation, where the roots are
        // sqrt((a+sqrt(|z|))/2)+i sqrt((-a+sqrt(|z|))/2), with the
        // real and imaginary parts having the same sign if im(z) is
        // positive and opposite signs if negative.  Set this up using
        // a ternary conditional based on the sign of im(z).
        Expression absZ = new ExprFunCall(param.getContext(), "abs", param);
        absZ = (Expression)absZ.accept(this);
        // NB: absZ is positive and real.
        Expression sqrtAbsZ = new ExprFunCall(absZ.getContext(), "sqrt", absZ);
        Expression a = param.getReal();
        Expression minusA = new ExprUnary(a.getContext(),
                                          ExprUnary.UNOP_NEG, a);

        Expression real = new ExprBinary(a.getContext(),
                                         ExprBinary.BINOP_ADD, a, sqrtAbsZ);
        real = new ExprBinary(real.getContext(), ExprBinary.BINOP_DIV,
                              real, new ExprConstInt(2));
        real = new ExprFunCall(real.getContext(), "sqrt", real);
        
        Expression imag = new ExprBinary(minusA.getContext(),
                                         ExprBinary.BINOP_ADD,
                                         minusA, sqrtAbsZ);
        imag = new ExprBinary(imag.getContext(), ExprBinary.BINOP_DIV,
                              imag, new ExprConstInt(2));
        imag = new ExprFunCall(imag.getContext(), "sqrt", imag);
        
        // Need to include logic to test the sign of the imaginary
        // part:
        Expression minusReal = new ExprUnary(real.getContext(),
                                             ExprUnary.UNOP_NEG, real);
        Expression imagPos = new ExprBinary(param.getImag().getContext(),
                                            ExprBinary.BINOP_GT,
                                            param.getImag(),
                                            new ExprConstInt(0));
        real = new ExprTernary(imagPos.getContext(), ExprTernary.TEROP_COND,
                               imagPos, real, minusReal);
        
        return new ExprComplex(fc.getContext(), real, imag);
    }

    public Expression fcCSqrt(ExprFunCall fc, Expression param)
    {
        return fcSqrt(fc, makeComplex(param));
    }
}
