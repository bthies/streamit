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
 * @version $Id: RenameBitVars.java,v 1.2 2003-04-05 16:11:38 dmaze Exp $
 */
public class RenameBitVars extends FEReplacer
{
    private SymbolTable symtab;
    
    public RenameBitVars()
    {
        this.symtab = null;
    }

    public Object visitExprVar(ExprVar var)
    {
        Type type = symtab.lookupVar(var.getName());
        if ((type instanceof TypePrimitive) &&
            (((TypePrimitive)type).getType() == TypePrimitive.TYPE_BIT))
            return new ExprVar(var.getContext(), "_bit_" + var.getName());
        return var;
    }

    public Object visitFunction(Function func)
    {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        Object result = super.visitFunction(func);
        symtab = oldSymTab;
        return result;
    }
    
    public Object visitFuncWork(FuncWork func)
    {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        Object result = super.visitFuncWork(func);
        symtab = oldSymTab;
        return result;
    }
    
    public Object visitStmtBlock(StmtBlock block)
    {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        Object result = super.visitStmtBlock(block);
        symtab = oldSymTab;
        return result;
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

    public Object visitStreamSpec(StreamSpec spec)
    {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        for (Iterator iter = spec.getParams().iterator(); iter.hasNext(); )
        {
            Parameter param = (Parameter)iter.next();
            symtab.registerVar(param.getName(), param.getType());
        }
        Object result = super.visitStreamSpec(spec);
        symtab = oldSymTab;
        return result;
    }

}
