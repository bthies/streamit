package streamit.frontend.tojava;

import streamit.frontend.nodes.*;
import streamit.frontend.passes.SymbolTableVisitor;

/**
 * Convert variables with a complex type into separate real and imaginary
 * parts.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: VarToComplex.java,v 1.10 2003-06-30 22:01:48 dmaze Exp $
 */
public class VarToComplex extends SymbolTableVisitor
{
    private static Expression makeComplexPair(Expression exp)
    {
        Expression real = new ExprField(exp.getContext(), exp, "real");
        Expression imag = new ExprField(exp.getContext(), exp, "imag");
        return new ExprComplex(exp.getContext(), real, imag);
    }

    public VarToComplex(SymbolTable st, StreamType strt)
    {
        super(st, strt);
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
        if (getType(exp).isComplex())
            return makeComplexPair(exp);
        // Otherwise recurse normally.
        return super.visitExprField(exp);
    }

    public Object visitExprArray(ExprArray exp)
    {
        // If the type of the expression is complex, decompose it;
        // otherwise, move on.
        if (getType(exp).isComplex())
            return makeComplexPair(exp);
        else
            return exp;
    }
}
