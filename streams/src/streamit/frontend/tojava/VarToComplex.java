/*
 * VarToComplex.java: split variables into separate real/complex parts
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: VarToComplex.java,v 1.1 2002-07-10 18:09:31 dmaze Exp $
 */

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;

/**
 * Convert variables with a complex type into separate real and imaginary
 * parts.
 */
public class VarToComplex extends FEReplacer
{
    private TJSymTab symtab;
    
    public VarToComplex(TJSymTab st)
    {
        symtab = st;
    }
    
    public Object visitExprVar(ExprVar exp)
    {
        String name = exp.getName();
        String type = symtab.lookup(name);
        if (type.equals("Complex"))
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
            String type = symtab.lookup(name);
            if (type.equals("Complex"))
                return exp;
        }
        // Otherwise recurse normally.
        return super.visitExprField(exp);
    }
}
