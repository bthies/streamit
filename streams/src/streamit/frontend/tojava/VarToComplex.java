/*
 * VarToComplex.java: split variables into separate real/complex parts
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: VarToComplex.java,v 1.4 2002-07-16 15:06:11 dmaze Exp $
 */

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;

/**
 * Convert variables with a complex type into separate real and imaginary
 * parts.
 */
public class VarToComplex extends FEReplacer
{
    private SymbolTable symtab;
    private GetExprType getExprType;
    
    public VarToComplex(SymbolTable st)
    {
        symtab = st;
        getExprType = new GetExprType(st);
    }
    
    public Object visitExprVar(ExprVar exp)
    {
        Type type = (Type)exp.accept(getExprType);
        if (type.isComplex())
        {
            Expression real = new ExprField(exp, "real");
            Expression imag = new ExprField(exp, "imag");
            return new ExprComplex(real, imag);
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
            Type type = symtab.lookup(name);
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
        Type type = (Type)exp.accept(getExprType);
        if (type.isComplex())
        {
            Expression real = new ExprField(exp, "real");
            Expression imag = new ExprField(exp, "imag");
            return new ExprComplex(real, imag);
        }
        else
            return exp;
    }
}
