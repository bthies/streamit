package streamit.frontend.passes;

import streamit.frontend.nodes.*;

import java.util.Iterator;

/**
 * Front-end visitor passes that renames variables of type
 * <code>bit</code> to have "_bit_" on the front of their names.  This
 * is needed as a workaround until we extend the main compiler
 * infrastructure to understand bit types.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: RenameBitVars.java,v 1.3 2003-04-09 15:26:49 dmaze Exp $
 */
public class RenameBitVars extends SymbolTableVisitor
{
    public RenameBitVars()
    {
        super(null);
    }

    public Object visitExprVar(ExprVar var)
    {
        Type type = symtab.lookupVar(var.getName());
        if ((type instanceof TypePrimitive) &&
            (((TypePrimitive)type).getType() == TypePrimitive.TYPE_BIT))
            return new ExprVar(var.getContext(), "_bit_" + var.getName());
        return var;
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        // Register the variable in the symbol table;
        symtab.registerVar(stmt.getName(), stmt.getType());
        // Then change the name if appropriate
        if ((stmt.getType() instanceof TypePrimitive) &&
            (((TypePrimitive)(stmt.getType())).getType() ==
             TypePrimitive.TYPE_BIT))
        {
            stmt = new StmtVarDecl(stmt.getContext(), stmt.getType(),
                                   "_bit_" + stmt.getName(), stmt.getInit());
            symtab.registerVar(stmt.getName(), stmt.getType());
        }
        return stmt;
    }
}
