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

import java.util.List;
import java.util.Map;

import java.util.HashMap;

/**
 * A hetereogeneous structure type.  This type has a name for itself,
 * and an ordered list of field names and types.  You can retrieve the
 * list of names, and the type a particular name maps to.  The names
 * must be unique within a given structure.
 *<p>
 * There is an important assumption in testing for equality and
 * type promotions: two structure types are considered equal if
 * they have the same name, regardless of any other characteristics.
 * This allows structures and associated structure-reference types
 * to sanely compare equal.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TypeStruct.java,v 1.5 2004-02-13 21:43:28 dmaze Exp $
 */
public class TypeStruct extends Type
{
    private FEContext context;
    private String name;
    private List fields;
    private Map types;
    
    /**
     * Creates a new structured type.  The fields and ftypes lists must
     * be the same length; a field in a given position in the fields
     * list has the type in the equivalent position in the ftypes list.
     *
     * @param context  file and line number the structure was declared in
     * @param name     name of the structure
     * @param fields   list of <code>String</code> containing the names
     *                 of the fields
     * @param ftypes   list of <code>Type</code> containing the types of
     *                 the fields
     */
    public TypeStruct(FEContext context, String name, List fields, List ftypes)
    {
        this.context = context;
        this.name = name;
        this.fields = fields;
        this.types = new HashMap();
        for (int i = 0; i < fields.size(); i++)
            this.types.put(fields.get(i), ftypes.get(i));
    }

    /**
     * Returns the context of the structure in the original source code.
     *
     * @return file name and line number the structure was declared in
     */
    public FEContext getContext()
    {
        return context;
    }
    
    /**
     * Returns the name of the structure.
     *
     * @return the name of the structure
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Returns the number of fields.
     *
     * @return the number of fields in the structure
     */
    public int getNumFields()
    {
        return fields.size();
    }
    
    /**
     * Returns the name of the specified field.
     *
     * @param n zero-based index of the field to get the name of
     * @return  the name of the nth field
     */
    public String getField(int n)
    {
        return (String)fields.get(n);
    }
    
    /**
     * Returns the type of the field with the specified name.
     *
     * @param f the name of the field to get the type of
     * @return  the type of the field named f
     */
    public Type getType(String f)
    {
        return (Type)types.get(f);
    }

    // Remember, equality and such only test on the name.
    public boolean equals(Object other)
    {
        if (other instanceof TypeStruct)
        {
            TypeStruct that = (TypeStruct)other;
            return this.name.equals(that.name);
        }
        
        if (other instanceof TypeStructRef)
        {
            TypeStructRef that = (TypeStructRef)other;
            return name.equals(that.getName());
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

