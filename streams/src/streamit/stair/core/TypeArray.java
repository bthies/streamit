package streamit.stair.core;

/**
 * Type representing a array of homogeneous objects.  The array may
 * have a fixed or unknown length.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TypeArray.java,v 1.1 2003-02-24 21:45:11 dmaze Exp $
 */
public class TypeArray extends Type
{
    private Type target;
    private int length;
    
    /**
     * Create an array type for a known length and target type.
     *
     * @param target  type of objects in the array
     * @param length  length of the array, or 0 if unknown
     */
    public TypeArray(Type target, int length)
    {
        this.target = target;
        this.length = length;
    }

    /**
     * Return the type of element in the array.
     *
     * @return  type of objects in the array
     */
    public Type getTarget()
    {
        return target;
    }
    
    /**
     * Return the length of the array.
     *
     * @return  number of elements in the array, or 0 if unknown
     */
    public int getLength()
    {
        return length;
    }
    
    /**
     * Returns the number of bits required to store this type.  This
     * is the bit width of the component types, multiplied by the
     * number of elements.  This may be 0 if the size of the target
     * type or the number of elements is unknown.  Also note that this
     * assumes dense packing of the target elements, which may not
     * actually be the case.
     *
     * @return  the width of the type, in bits, or 0 for unknown
     */
    public int getBitWidth()
    {
        return target.getBitWidth() * length;
    }

    /**
     * Returns true if this can be converted to that without penalty.
     * This is true iff this has indeterminate length or that's length
     * is at least as long as this's, and if the target type of this
     * is equal to the target type of that.  (Without the type
     * equality constraint, the memory layout of the array might
     * change.)
     *
     * @param that  type to compare to
     * @return      true if this can be converted to that without penalty
     */
    public boolean isConvertibleTo(Type that)
    {
        if (!(that instanceof TypeArray))
            return false;
        TypeArray ta = (TypeArray)that;
        if (!(this.getTarget().equals(ta.getTarget())))
            return false;
        if (this.getLength() > ta.getLength())
            return false;
        return true;
    }

    /**
     * Returns true if this is equal to that.  This is true iff that
     * is an array type with the same target and length as this.
     *
     * @param that  type to compare to
     * @return      true if this and that are the same type
     */
    public boolean equals(Object that)
    {
        if (!(that instanceof TypeArray))
            return false;
        TypeArray ta = (TypeArray)that;
        if (!(this.getTarget().equals(that.getTarget())))
            return false;
        if (this.getLength() != that.getLength())
            return false;
        return true;
    }

    /**
     * Returns a hash code value for the object.  This uses the target
     * type's hash code and the array length to produce a number that
     * is equal for two equal types.
     *
     * @return  a hash code value for this object
     */
    public int hashCode()
    {
        return target.hashCode() * (length + 1);
    }
}
