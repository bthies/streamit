/*
 * TypeStructRef.java: a reference-by-name to a structure type
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: TypeStructRef.java,v 1.1 2002-08-23 13:22:17 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A named reference to a structure type, as defined in TypeStruct.
 * This will be produced directly by the parser, but later passes
 * should replace these with TypeStructs as appropriate.
 */
public class TypeStructRef extends Type
{
    private String name;
    
    /** Creates a new reference to a structured type. */
    public TypeStructRef(String name)
    {
        this.name = name;
    }
    
    /** Returns the name of the referenced structure. */
    public String getName()
    {
        return name;
    }
}
