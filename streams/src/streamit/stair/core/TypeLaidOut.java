package streamit.stair.core;

import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * A composite type containing heterogeneous child types with explicit
 * locations within the structure.  This is an ordered listing of
 * triples of names, types, and bit offsets.  The type is technically
 * mutable for ease of construction, but beyond the initial
 * construction it should not be changed.  This class can represent C
 * structs or unions; if it represents a structure, it is a
 * representation of the structure on the target platform with
 * explicit layout.
 *
 * @see     TypeStruct
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TypeLaidOut.java,v 1.1 2003-02-24 21:45:11 dmaze Exp $
 */
public class TypeLaidOut extends Type
{
    // names is a list of String; types is a list of Type;
    // offsets is a list of Integer
    private List names, types, offsets;
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
    public TypeLaidOut(String name)
    {
        names = new ArrayList();
        types = new ArrayList();
        offsets = new ArrayList();
        myName = name;
    }
    
    /**
     * Add a (name, type, offset) triple to the list of fields for
     * this structure.  The type should be a concrete type with an
     * explicit bit width.
     *
     * @param name    string name of the field
     * @param type    type of the field
     * @param offset  location of the field within the structure, in bits
     */
    public void addField(String name, Type type, int offset)
    {
        names.add(name);
        types.add(type);
        offsets.add(new Integer(offset));
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
     * Get the bit position of the nth field.
     *
     * @param n  number of the field to return
     * @return   offset from the start of the structure, in bits
     * @throws   IndexOutOfBoundsException - if the index is out of range
     */
    public int getOffset(int n)
    {
        return ((Integer)offsets.get(n)).intValue();
    }

    /**
     * Return the number of bits required to store this type.  This is
     * the maximum of any sum of a bit offset and the width of the
     * associated type.
     *
     * @return  the width of the type, in bits
     */
    public int getBitWidth()
    {
        int size = 0;
        for (int i = 0; i < getNumFields(); i++)
        {
            int candidate = getOffset(n) + getType(n).getBitWidth();
            if (candidate > size)
                size = candidate;
        }
        return size;
    }

    /**
     * Return true if this can be converted to that without penalty.
     * This is true if that is also a laid-out type, and that's
     * fields are a superset of this's and in the same order.
     *
     * @param that  type to compare to
     * @return      true if this can be converted to that without penalty
     */
    public boolean isConvertibleTo(Type that)
    {
        if (!(that instanceof TypeLaidOut))
            return false;
        TypeLaidOut tl = (TypeLaidOut)that;
        // that must have at least as many fields as this.
        if (that.getNumFields() < this.getNumFields())
            return false;
        // loop through:
        for (int i = 0; i < getNumFields(); i++)
        {
            if (!(getName(i).equals(tl.getName(i))))
                return false;
            if (!(getType(i).isConvertibleTo(tl.getType(i))))
                return false;
            if (getOffset(i) != tl.getOffset(i))
                return false;
        }
        return true;
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
        if (!(that instanceof TypeLaidOut))
            return false;
        TypeLaidOut tl = (TypeLaidOut)that;
        // that must have exactly as many fields as this.
        if (that.getNumFields() != this.getNumFields())
            return false;
        // loop through:
        for (int i = 0; i < getNumFields(); i++)
        {
            if (!(getName(i).equals(tl.getName(i))))
                return false;
            if (!(getType(i).equals(tl.getType(i))))
                return false;
            if (getOffset(i) != tl.getOffset(i))
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
