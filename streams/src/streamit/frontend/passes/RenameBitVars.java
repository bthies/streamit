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
 * @version $Id: RenameBitVars.java,v 1.6 2003-10-09 19:51:01 dmaze Exp $
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
