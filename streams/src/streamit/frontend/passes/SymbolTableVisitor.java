package streamit.frontend.passes;

import streamit.frontend.nodes.*;

import java.util.Iterator;

/**
 * Front-end visitor pass that maintains a symbol table.  Other
 * passes that need symbol table information can extend this.
 * The protected <code>symtab</code> member has the prevailing
 * symbol table as each node is visited.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: SymbolTableVisitor.java,v 1.1 2003-04-09 15:26:49 dmaze Exp $
 */
public class SymbolTableVisitor extends FEReplacer
{
    /**
     * The current symbol table.  Functions in this class keep the
     * symbol table up to date; calling
     * <code>super.visitSomething</code> from a derived class will
     * update the symbol table if necessary and recursively visit
     * children.
     */
    protected SymbolTable symtab;
    
    /**
     * Create a new symbol table visitor.
     *
     * @param symtab  Symbol table to use if no other is available,
     *                can be null
     */
    public SymbolTableVisitor(SymbolTable symtab)
    {
        this.symtab = symtab;
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
        symtab.registerVar(stmt.getName(), stmt.getType());
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
