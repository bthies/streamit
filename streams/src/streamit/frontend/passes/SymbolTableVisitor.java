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
 * @version $Id: SymbolTableVisitor.java,v 1.9 2003-07-07 21:28:12 dmaze Exp $
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
     * The current stream type.  Functions in this class keep the
     * prevailing stream type up to date, but anonymous streams may
     * have a null stream type.  Calling a visitor method will update
     * the stream type if necessary and recursively visit children.
     */
    protected StreamType streamType;

    /**
     * Create a new symbol table visitor.
     *
     * @param symtab  Symbol table to use if no other is available,
     *                can be null
     */
    public SymbolTableVisitor(SymbolTable symtab)
    {
        this.symtab = symtab;
        this.streamType = null;
    }

    /**
     * Create a new symbol table visitor.
     *
     * @param symtab  Symbol table to use if no other is available,
     *                can be null
     * @param st      Prevailing stream type, can be null
     */
    public SymbolTableVisitor(SymbolTable symtab, StreamType st)
    {
        this.symtab = symtab;
        this.streamType = st;
    }

    /**
     * Get the type of an <code>Expression</code>.
     *
     * @param expr  Expression to get the type of
     * @returns     Type of the expression
     * @see         streamit.frontend.nodes.GetExprType
     */
    public Type getType(Expression expr)
    {
        // To think about: should we cache GetExprType objects?
        return (Type)expr.accept(new GetExprType(symtab, streamType));
    }

    public Object visitFieldDecl(FieldDecl field)
    {
        for (int i = 0; i < field.getNumFields(); i++)
            symtab.registerVar(field.getName(i), field.getType(i), field,
                               SymbolTable.KIND_FIELD);
        return super.visitFieldDecl(field);
    }

    public Object visitFunction(Function func)
    {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        for (Iterator iter = func.getParams().iterator(); iter.hasNext(); )
        {
            Parameter param = (Parameter)iter.next();
            symtab.registerVar(param.getName(), param.getType(), param,
                               SymbolTable.KIND_FUNC_PARAM);
        }
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
        for (int i = 0; i < stmt.getNumVars(); i++)
            symtab.registerVar(stmt.getName(i), stmt.getType(i), stmt,
                               SymbolTable.KIND_LOCAL);
        return super.visitStmtVarDecl(stmt);
    }

    public Object visitStreamSpec(StreamSpec spec)
    {
        StreamType oldStreamType = streamType;
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        streamType = spec.getStreamType();
        for (Iterator iter = spec.getParams().iterator(); iter.hasNext(); )
        {
            Parameter param = (Parameter)iter.next();
            symtab.registerVar(param.getName(), param.getType(), param,
                               SymbolTable.KIND_STREAM_PARAM);
        }
        Object result = super.visitStreamSpec(spec);
        symtab = oldSymTab;
        streamType = oldStreamType;
        return result;
    }

}
