/*
 * SymbolTable.java: symbol table for StreamIt programs
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: SymbolTable.java,v 1.1 2002-07-15 19:14:15 dmaze Exp $
 */

package streamit.frontend.nodes;

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
    private Map symbols;
    private SymbolTable parent;
    
    /** Creates a new symbol table with the specified parent (possibly
     * null). */
    public SymbolTable(SymbolTable parent)
    {
        symbols = new HashMap();
        this.parent = parent;
    }

    /** Registers a new symbol in the symbol table. */
    public void register(String name, Type type)
    {
        symbols.put(name, type);
    }
    
    /** Looks up the type for a symbol.  If that symbol is not in the
     * current symbol table, search in the parent.  If the parent is null,
     * returns null. */
    public Type lookup(String name)
    {
        Type type = (Type)symbols.get(name);
        if (type != null)
            return type;
        if (parent != null)
            return parent.lookup(name);
        return null;
    }

    /** Returns the parent of this, or null if this has no parent. */
    public SymbolTable getParent()
    {
        return parent;
    }
}
