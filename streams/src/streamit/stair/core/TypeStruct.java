package streamit.stair.core;

/**
 * A composite type containing heterogeneous child types, as in a C
 * structure.  This is an ordered listing of pairs of names and types,
 * with no explicit layout information.  The type is technically
 * mutable for ease of construction, but beyond the initial
 * construction it should not be changed.  A structured type with
 * explicit layout information (<i>e.g.</i>, bit offsets of members)
 * or a C union type is represented by a {@link TypeLaidOut} instead.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TypeStruct.java,v 1.1 2003-02-24 21:45:11 dmaze Exp $
 */
public class TypeStruct extends Type
{
    // names is a list of String; types is a list of Type
    private List names, types;
    // optional name of this type
    private String myName;
    
    /**
     * Create a new structure type with no members.  After
     * construction, call {@link #addField} repeatedly with the names
     * and types of members of the structure.
     *
     * @param name  string name of the structure in the original language,
     *              or null
     */
    public TypeStruct(String name)
    {
        names = new ArrayList();
        types = new ArrayList();
        myName = name;
    }
    
    /**
     * Add a (name, type) pair to the list of fields for this structure.
     *
     * @param name  string name of the field
     * @param type  type of the field
     */
    public void addField(String name, Type type)
    {
        names.add(name);
        types.add(type);
    }

    /**
     * Get the number of fields in this structure.
     *
     * @return  the number of fields
     */
    public int getNumFields()
    {
        return names.size();
    }
    
    /**
     * Get the name of the nth field.
     *
     * @param n  number of the field to return
     * @return   string name of the field
     * @throws   IndexOutOfBoundsException - if the index is out of range
     */
    public String getName(int n)
    {
        return (String)names.get(n);
    }
    
    /**
     * Get the type of the nth field.
     *
     * @param n  number of the field to return
     * @return   type of the field
     * @throws   IndexOutOfBoundsException - if the index is out of range
     */
    public Type getType(int n)
    {
        return (Type)types.get(n);
    }

    /**
     * Return the minimum number of bits required to store this type.
     * This is necessarily only an estimate; it is the sum of the bit
     * widths of the members.  If any of the members of this have
     * unknown bit width, the entire structure has an unknown width.
     * When the type is actually laid out, a more accurate bit width
     * can be determined.
     *
     * @return  the width  of the type, in bits, or 0 for unknown
     * @see     TypeLaidOut#getBitWidth
     */
    public int getBitWidth()
    {
        int size = 0;
        for (Iterator iter = types.iterator(); iter.hasNext(); )
        {
            Type type = (Type)iter.next();
            int width = type.getBitWidth();
            if (width == 0)
                return 0;
            size += width;
        }
        return size;
    }

    /**
     * Return true if this can be converted to that without penalty.
     * This is true if that is also a structured type, and that's
     * fields are a superset of this's and in the same order.
     *
     * @param that  type to compare to
     * @return      true if this can be converted to that without penalty
     */
    public boolean isConvertibleTo(Type that)
    {
        // Two cases that are interesting: either (1) that is also
        // a TypeStruct that is a superset of this, or (2) that is a
        // TypeLaidOut that is a valid layout of this.
        if (that instanceof TypeStruct)
        {
            TypeStruct ts = (TypeStruct)that;
            // that must have at least as many fields as this.
            if (that.getNumFields() < this.getNumFields())
                return false;
            // loop through:
            for (int i = 0; i < getNumFields(); i++)
            {
                if (!(getName(i).equals(ts.getName(i))))
                    return false;
                if (!(getType(i).isConvertibleTo(ts.getType(i))))
                    return false;
            }
            return true;
        }
        else
            return false;
    }

    /**
     * Returns true if this is equal to that.  This is true iff that
     * is a structured type with the same fields as this.
     *
     * @param that  type to compare to
     * @return      true if this and that are the same type
     */
    public boolean equals(Object that)
    {
        if (!(that instanceof TypeStruct))
            return false;
        TypeStruct ts = (TypeStruct)that;
        // that must have exactly as many fields as this.
        if (that.getNumFields() != this.getNumFields())
            return false;
        // loop through:
        for (int i = 0; i < getNumFields(); i++)
        {
            if (!(getName(i).equals(ts.getName(i))))
                return false;
            if (!(getType(i).equals(ts.getType(i))))
                return false;
        }
        return true;
    }

    /**
     * Return a hash code value for the object.  This combines the
     * hash codes of the member names and types somewhat arbitrarily.
     *
     * @return  a hash code value for this object
     */
    public int hashCode()
    {
        // Perhaps hashCode() shouldn't be O(n) in the number of
        // fields, but we expect the number of fields to generally
        // be "small".
        int code = 0;
        for (int i = 0; i < getNumFields(); i++)
        {
            code <<= 1;
            code ^= getName(i).hashCode();
            code <<= 1;
            code ^= getType(i).hashCode();
        }
        return code;
    }
}
