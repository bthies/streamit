/*
 * ComplexProp.java: cause complex values to bubble upwards
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: ComplexProp.java,v 1.2 2002-07-10 21:02:25 dmaze Exp $
 */

// Does this actually belong here?  If we evolve more front-end passes,
// it can move.  --dzm
package streamit.frontend.nodes;

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
            return new ExprComplex(exp, new ExprConstFloat(0.0));
        ExprComplex cplx = (ExprComplex)exp;
        if (cplx.getReal() != null && cplx.getImag() != null)
            return cplx;
        Expression real, imag;
        real = cplx.getReal();
        if (real == null) real = new ExprConstFloat(0.0);
        imag = cplx.getImag();
        if (imag == null) imag = new ExprConstFloat(0.0);
        return new ExprComplex(real, imag);
    }

    public Object visitExprBinary(ExprBinary exp)
    {
        Expression left = (Expression)exp.getLeft().accept(this);
        Expression right = (Expression)exp.getRight().accept(this);
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
            case ExprBinary.BINOP_SUB:
                if (lr == null)
                    real = rr;
                else if (rr == null)
                    real = lr;
                else
                    real = new ExprBinary(exp.getOp(), lr, rr);
                
                if (li == null)
                    imag = ri;
                else if (ri == null)
                    imag = li;
                else
                    imag = new ExprBinary(exp.getOp(), li, ri);
                
                return new ExprComplex(real, imag);

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
                
                ar = new ExprBinary(ExprBinary.BINOP_MUL, lr, rr);
                br = new ExprBinary(ExprBinary.BINOP_MUL, li, ri);
                real = new ExprBinary(ExprBinary.BINOP_SUB, ar, br);
                
                ai = new ExprBinary(ExprBinary.BINOP_MUL, lr, ri);
                bi = new ExprBinary(ExprBinary.BINOP_MUL, li, rr);
                imag = new ExprBinary(ExprBinary.BINOP_ADD, ai, bi);
                
                return new ExprComplex(real, imag);
                
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

                ar = new ExprBinary(ExprBinary.BINOP_MUL, rr, rr);
                br = new ExprBinary(ExprBinary.BINOP_MUL, ri, ri);
                denom = new ExprBinary(ExprBinary.BINOP_SUB, ar, br);
                
                ar = new ExprBinary(ExprBinary.BINOP_MUL, lr, rr);
                br = new ExprBinary(ExprBinary.BINOP_MUL, li, ri);
                real = new ExprBinary(ExprBinary.BINOP_ADD, ar, br);
                real = new ExprBinary(ExprBinary.BINOP_DIV, real, denom);
                
                ai = new ExprBinary(ExprBinary.BINOP_MUL, li, rr);
                bi = new ExprBinary(ExprBinary.BINOP_MUL, lr, ri);
                imag = new ExprBinary(ExprBinary.BINOP_SUB, ai, bi);
                imag = new ExprBinary(ExprBinary.BINOP_DIV, imag, denom);
                
                return new ExprComplex(real, imag);
                
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

                left = new ExprBinary(ExprBinary.BINOP_EQ, lr, rr);
                right = new ExprBinary(ExprBinary.BINOP_EQ, li, ri);
                return new ExprBinary(ExprBinary.BINOP_AND, left, right);

            case ExprBinary.BINOP_NEQ:
                // Need to construct (lr != rr) || (li != ri),
                // forcing things to be 0.0 if needed.
                if (lr == null) lr = new ExprConstFloat(0.0);
                if (li == null) li = new ExprConstFloat(0.0);
                if (rr == null) rr = new ExprConstFloat(0.0);
                if (ri == null) ri = new ExprConstFloat(0.0);

                left = new ExprBinary(ExprBinary.BINOP_NEQ, lr, rr);
                right = new ExprBinary(ExprBinary.BINOP_NEQ, li, ri);
                return new ExprBinary(ExprBinary.BINOP_OR, left, right);
                
                // Anything else is wrong; punt on the problem.
            default:
            }

            // Reconstruct the left and right sides if we need to.
            // For now, always recreate them (we need to test whether
            // left and right are complex).
            // if (lr != left.getReal() || li != left.getImag())
            left = new ExprComplex(lr, li);
            // if (rr != right.getReal() || ri != right.getImag())
            right = new ExprComplex(rr, ri);
        }
        // Now build the new result, but only if we need to.
        if (left == exp.getLeft() && right == exp.getRight())
            return exp;
        else
            return new ExprBinary(exp.getOp(), left, right);
    }

    public Object visitExprTernary(ExprTernary exp)
    {
        // Only one case of these, so don't bother checking the op.
        Expression a = (Expression)exp.getA().accept(this);
        Expression b = (Expression)exp.getB().accept(this);
        Expression c = (Expression)exp.getC().accept(this);

        if (a == exp.getA() && b == exp.getB() && c == exp.getC())
            return exp;

        // Now, we assume that a is real.  If neither b nor c is
        // complex, then the whole thing is real:
        if (!(b instanceof ExprComplex) && !(c instanceof ExprComplex))
            return new ExprTernary(exp.getOp(), a, b, c);

        // Otherwise, force both b and c to be complex.
        ExprComplex bc = makeComplex(b);
        ExprComplex cc = makeComplex(c);
        
        // Now create the pair of ternary expressions.
        Expression real = new ExprTernary(exp.getOp(), a, bc.getReal(), cc.getReal());
        Expression imag = new ExprTernary(exp.getOp(), a, bc.getImag(), cc.getImag());
        return new ExprComplex(real, imag);
    }

    public Object visitExprUnary(ExprUnary exp)
    {
        // The only operation this makes sense for is negation,
        // and we're ignoring error-checking everywhere else anyways.
        // So assume every unary operation is like negation.  In that
        // case, -(a+bi) == (-a)+(-b)i.
        Expression expr = (Expression)exp.getExpr().accept(this);
        if (expr == exp.getExpr())
            return exp;
        if (!(expr instanceof ExprComplex))
            return new ExprUnary(exp.getOp(), expr);
        ExprComplex cplx = makeComplex(expr);
        Expression real = new ExprUnary(exp.getOp(), cplx.getReal());
        Expression imag = new ExprUnary(exp.getOp(), cplx.getImag());
        return new ExprComplex(real, imag);
    }
}
