/*
 * SymbolTable.java: symbol table for StreamIt programs
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: SymbolTable.java,v 1.3 2002-11-20 20:43:54 dmaze Exp $
 */

package streamit.frontend.nodes;

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

    /** Returns the parent of this, or null if this has no parent. */
    public SymbolTable getParent()
    {
        return parent;
    }
}
