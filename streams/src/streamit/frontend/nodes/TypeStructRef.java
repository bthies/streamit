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

package streamit.frontend.nodes;

/**
 * A named reference to a structure type, as defined in TypeStruct.
 * This will be produced directly by the parser, but later passes
 * should replace these with TypeStructs as appropriate.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TypeStructRef.java,v 1.3 2004-02-13 21:41:53 dmaze Exp $
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

    public boolean equals(Object other)
    {
        if (other instanceof TypeStruct)
        {
            TypeStruct that = (TypeStruct)other;
            return name.equals(that.getName());
        }
        
        if (other instanceof TypeStructRef)
        {
            TypeStructRef that = (TypeStructRef)other;
            return this.name.equals(that.name);
        }
        
        if (this.isComplex() && other instanceof Type)
            return ((Type)other).isComplex();
        
        return false;
    }
    
    public int hashCode()
    {
        return name.hashCode();
    }
    
    public String toString()
    {
        return name;
    }
}
