/*
 * SymbolTable.java: symbol table for StreamIt programs
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: SymbolTable.java,v 1.4 2002-11-20 21:49:10 dmaze Exp $
 */

package streamit.frontend.nodes;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.HashMap;

/**
 * A symbol table for StreamIt programs.  This keeps a mapping from a
 * string name to a front-end type, and has a parent symbol table
 * (possibly null).  A name can be registered in the current symbol
 * table.  When resolving a name's type, the name is searched for
 * first in the current symbol table, and if not present than in the
 * parent symbol table.
 */
public class SymbolTable
{
    private Map vars, fns;
    private SymbolTable parent;
    private List includedFns;
    
    /** Creates a new symbol table with the specified parent (possibly
     * null). */
    public SymbolTable(SymbolTable parent)
    {
        vars = new HashMap();
        fns = new HashMap();
        this.parent = parent;
        this.includedFns = null;
    }

    /** Registers a new symbol in the symbol table. */
    public void registerVar(String name, Type type)
    {
        vars.put(name, type);
    }

    /** Registers a new function in the symbol table. */
    public void registerFn(Function fn)
    {
        // Ignore null-named functions.
        if (fn.getName() != null)
            fns.put(fn.getName(), fn);
    }
    
    /** Looks up the type for a symbol.  If that symbol is not in the
     * current symbol table, search in the parent.  If the parent is null,
     * returns null. */
    public Type lookupVar(String name)
    {
        Type type = (Type)vars.get(name);
        if (type != null)
            return type;
        if (parent != null)
            return parent.lookupVar(name);
        throw new UnrecognizedVariableException(name);
    }

    /** Looks up the function corresponding to a particular name.  If
     * that name is not in the symbol table, searches the parent, and
     * then each of the symbol tables in includedFns, depth-first, in
     * order.  Throws an UnrecognizedVariableException if the function
     * doesn't exist. */
    public Function lookupFn(String name)
    {
        Function fn = doLookupFn(name);
        if (fn != null) return fn;
        throw new UnrecognizedVariableException(name);
    }

    private Function doLookupFn(String name)
    {
        Function fn = (Function)fns.get(name);
        if (fn != null)
            return fn;
        if (parent != null)
        {
            fn = parent.doLookupFn(name);
            if (fn != null)
                return fn;
        }
        if (includedFns != null)
        {
            for (Iterator iter = includedFns.iterator(); iter.hasNext(); )
            {
                SymbolTable other = (SymbolTable)iter.next();
                fn = other.doLookupFn(name);
                if (fn != null)
                    return fn;
            }
        }
        return null;
    }

    /** Returns the parent of this, or null if this has no parent. */
    public SymbolTable getParent()
    {
        return parent;
    }
}
