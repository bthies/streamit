/*
 * TJSymTab.java: minimal symbol table for StreamIt->Java conversion
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: TJSymTab.java,v 1.2 2002-07-15 18:58:17 dmaze Exp $
 */

package streamit.frontend.tojava;

import java.util.Map;
import streamit.frontend.nodes.Type;

import java.util.HashMap;

/**
 * A minimal symbol table for StreamIt-to-Java conversion.  This keeps
 * a mapping from a string name to a front-end type, and has a parent symbol
 * table (possibly null).  A name can be registered in the current
 * symbol table.  When resolving a name's type, the name is searched for
 * first in the current symbol table, and if not present than in the
 * parent symbol table.
 */
public class TJSymTab
{
    private Map symbols;
    private TJSymTab parent;
    
    /** Creates a new symbol table with the specified parent (possibly
     * null). */
    public TJSymTab(TJSymTab parent)
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
    public TJSymTab getParent()
    {
        return parent;
    }
}
