/*
 * VarToComplex.java: split variables into separate real/complex parts
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: VarToComplex.java,v 1.3 2002-07-15 19:14:17 dmaze Exp $
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
    
    public VarToComplex(SymbolTable st)
    {
        symtab = st;
    }
    
    public Object visitExprVar(ExprVar exp)
    {
        String name = exp.getName();
        Type type = symtab.lookup(name);
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
}
