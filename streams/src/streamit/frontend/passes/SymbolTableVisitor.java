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
import java.util.Map;

/**
 * Front-end visitor pass that maintains a symbol table.  Other
 * passes that need symbol table information can extend this.
 * The protected <code>symtab</code> member has the prevailing
 * symbol table as each node is visited.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: SymbolTableVisitor.java,v 1.15 2004-05-04 15:00:21 thies Exp $
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
     * Map resolving structure names to structure types.  This map is
     * used early in the front end: if code needs to resolve the type
     * of a structure variable that only has a structure-reference
     * type, before NoRefTypes has been run, this can perform that
     * resolution.  It is populated by <code>visitProgram()</code>.
     */
    protected Map structsByName;

    /**
     * Create a new symbol table visitor.
     *
     * @param symtab  Symbol table to use if no other is available,
     *                can be null
     */
    public SymbolTableVisitor(SymbolTable symtab)
    {
        this(symtab, null);
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
        this.structsByName = new java.util.HashMap();
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
        GetExprType get = new GetExprType(symtab, streamType, structsByName);
        Type type = (Type)expr.accept(get);
        return actualType(type);
    }

    /**
     * Add a variable declaration and register the variable in the
     * symbol table.  This creates a {@link
     * streamit.frontend.nodes.StmtVarDecl} for the specified type and
     * name, and adds that statement using {@link addStatement}.  It
     * also registers the new variable in the current symbol table.
     *
     * @param context  file and line number the statement belongs to
     * @param type     type of the variable
     * @param name     name of the variable
     */
    protected void addVarDecl(FEContext context, Type type, String name)
    {
        Statement stmt = new StmtVarDecl(context, type, name, null);
        addStatement(stmt);
        symtab.registerVar(name, type, stmt, SymbolTable.KIND_LOCAL);
    }

    /**
     * Get the actual type for a type.  In particular, if we have a
     * structure-reference type and the name of the reference is
     * registered, then the actual type is the corresponding
     * structure type.
     *
     * @param type  type to resolve to actual type
     * @return      actual resolved type
     */
    protected Type actualType(Type type)
    {
        if (type instanceof TypeStructRef)
        {
            String name = ((TypeStructRef)type).getName();
            if (structsByName.containsKey(name))
                type = (Type)structsByName.get(name);
        }
        return type;
    }

    public Object visitFieldDecl(FieldDecl field)
    {
        for (int i = 0; i < field.getNumFields(); i++)
            symtab.registerVar(field.getName(i),
                               actualType(field.getType(i)),
                               field,
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
            symtab.registerVar(param.getName(),
                               actualType(param.getType()),
                               param,
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

    public Object visitProgram(Program prog)
    {
        // Examine and register structure members, then recurse normally.
        for (Iterator iter = prog.getStructs().iterator(); iter.hasNext(); )
        {
            TypeStruct struct = (TypeStruct)iter.next();
            structsByName.put(struct.getName(), struct);
        }
        return super.visitProgram(prog);
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
            symtab.registerVar(stmt.getName(i),
                               actualType(stmt.getType(i)),
                               stmt,
                               SymbolTable.KIND_LOCAL);
        return super.visitStmtVarDecl(stmt);
    }

    public Object visitStreamSpec(StreamSpec spec)
    {
        StreamType oldStreamType = streamType;
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        streamType = spec.getStreamType();
	// register parameters
        for (Iterator iter = spec.getParams().iterator(); iter.hasNext(); )
        {
            Parameter param = (Parameter)iter.next();
            symtab.registerVar(param.getName(),
                               actualType(param.getType()),
                               param,
                               SymbolTable.KIND_STREAM_PARAM);
        }
	// register functions
        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
        {
	    Function func = (Function)iter.next();
	    symtab.registerFn(func);
	}
        Object result = super.visitStreamSpec(spec);
        symtab = oldSymTab;
        streamType = oldStreamType;
        return result;
    }

}
