package streamit.frontend.nodes;

import java.util.List;
import java.util.Map;

import java.util.HashMap;

/**
 * A hetereogeneous structure type.  This type has a name for itself,
 * and an ordered list of field names and types.  You can retrieve the
 * list of names, and the type a particular name maps to.  The names
 * must be unique within a given structure.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TypeStruct.java,v 1.2 2003-07-08 20:44:24 dmaze Exp $
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
     * @returns file name and line number the structure was declared in
     */
    public FEContext getContext()
    {
        return context;
    }
    
    /**
     * Returns the name of the structure.
     *
     * @returns the name of the structure
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Returns the number of fields.
     *
     * @returns the number of fields in the structure
     */
    public int getNumFields()
    {
        return fields.size();
    }
    
    /**
     * Returns the name of the specified field.
     *
     * @param n zero-based index of the field to get the name of
     * @returns the name of the nth field
     */
    public String getField(int n)
    {
        return (String)fields.get(n);
    }
    
    /**
     * Returns the type of the field with the specified name.
     *
     * @param f the name of the field to get the type of
     * @returns the type of the field named f
     */
    public Type getType(String f)
    {
        return (Type)types.get(f);
    }
}

