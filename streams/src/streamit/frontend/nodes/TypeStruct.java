/*
 * TypeStruct.java: a heterogeneous structure type
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: TypeStruct.java,v 1.1 2002-07-15 15:44:08 dmaze Exp $
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
 */
public class TypeStruct extends Type
{
    private String name;
    private List fields;
    private Map types;
    
    /** Creates a new structured type.  fields must be a List of Strings;
     * ftypes is a List of the same length as fields of Types.  This creates
     * a type named name; its fields are as named in fields, and a field
     * fields[i] has type ftypes[i]. */
    public TypeStruct(String name, List fields, List ftypes)
    {
        this.name = name;
        this.fields = fields;
        this.types = new HashMap();
        for (int i = 0; i < fields.size(); i++)
            this.types.put(fields.get(i), ftypes.get(i));
    }
    
    /** Returns the name of the structure. */
    public String getName()
    {
        return name;
    }
    
    /** Returns the number of fields. */
    public int getNumFields()
    {
        return fields.size();
    }
    
    /** Returns the name of the specified field. */
    public String getField(int n)
    {
        return (String)fields.get(n);
    }
    
    /** Returns the type of the field with the specified name. */
    public Type getType(String f)
    {
        return (Type)types.get(f);
    }
}

