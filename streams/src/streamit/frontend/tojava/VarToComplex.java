package streamit.frontend.tojava;

import streamit.frontend.nodes.*;
import streamit.frontend.passes.SymbolTableVisitor;

/**
 * Convert variables with a complex type into separate real and imaginary
 * parts.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: VarToComplex.java,v 1.8 2003-06-24 21:09:51 dmaze Exp $
 */
public class VarToComplex extends SymbolTableVisitor
{
    public VarToComplex(SymbolTable st, StreamType strt)
    {
        super(st, strt);
    }
    
    public Object visitExprVar(ExprVar exp)
    {
        Type type = (Type)exp.accept(new GetExprType(symtab, streamType));
        if (type.isComplex())
        {
            Expression real = new ExprField(exp.getContext(), exp, "real");
            Expression imag = new ExprField(exp.getContext(), exp, "imag");
            return new ExprComplex(exp.getContext(), real, imag);
        }
        else
            return exp;
    }

    public Object visitExprField(ExprField exp)
    {
        // If the expression is already visiting a field of a Complex
        // object, don't recurse further.
        if (exp.getLeft() instanceof ExprVar)
        {
            ExprVar left = (ExprVar)exp.getLeft();
            String name = left.getName();
            Type type = symtab.lookupVar(name);
            if (type.isComplex())
                return exp;
        }
        // Otherwise recurse normally.
        return super.visitExprField(exp);
    }

    public Object visitExprArray(ExprArray exp)
    {
        // If the type of the expression is complex, decompose it;
        // otherwise, move on.
        Type type = (Type)exp.accept(new GetExprType(symtab, streamType));
        if (type.isComplex())
        {
            Expression real = new ExprField(exp.getContext(), exp, "real");
            Expression imag = new ExprField(exp.getContext(), exp, "imag");
            return new ExprComplex(exp.getContext(), real, imag);
        }
        else
            return exp;
    }
}
