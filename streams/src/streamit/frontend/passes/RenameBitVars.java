package streamit.frontend.passes;

import streamit.frontend.nodes.*;

import java.util.Iterator;
import java.util.List;

/**
 * Front-end visitor passes that renames variables of type
 * <code>bit</code> to have "_bit_" on the front of their names.  This
 * is needed as a workaround until we extend the main compiler
 * infrastructure to understand bit types.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: RenameBitVars.java,v 1.5 2003-07-07 21:21:42 dmaze Exp $
 */
public class RenameBitVars extends SymbolTableVisitor
{
    public RenameBitVars()
    {
        super(null);
    }

    public Object visitExprVar(ExprVar var)
    {
        Type type = symtab.lookupVar(var);
        if ((type instanceof TypePrimitive) &&
            (((TypePrimitive)type).getType() == TypePrimitive.TYPE_BIT))
            return new ExprVar(var.getContext(), "_bit_" + var.getName());
        return var;
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        // Register the variable in the symbol table;
        stmt = (StmtVarDecl)super.visitStmtVarDecl(stmt);
        // Then change the name if appropriate
        List newNames = new java.util.ArrayList();
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            String name = stmt.getName(i);
            Type type = stmt.getType(i);
            if ((type instanceof TypePrimitive) &&
                (((TypePrimitive)(type)).getType() == TypePrimitive.TYPE_BIT))
            {
                name = "_bit_" + name;
                symtab.registerVar(name, type, stmt, SymbolTable.KIND_LOCAL);
            }
            newNames.add(name);
        }
        return new StmtVarDecl(stmt.getContext(), stmt.getTypes(),
                               newNames, stmt.getInits());
    }
}
