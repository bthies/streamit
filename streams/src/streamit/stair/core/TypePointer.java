package streamit.stair.core;

/**
 * Type representing a pointer to some other typed object in memory.
 * A pointer type has a bit width, which is typically the address
 * width of the target machine, and a type being pointed to.  The
 * target type may be null for a pointer to memory (in C parlance,
 * <code>void *</code>).
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu &gt;
 * @version $Id: TypePointer.java,v 1.1 2003-02-24 21:45:11 dmaze Exp $
 */
public class TypePointer extends Type
{
    public Type target;
    public int width;
    
    /**
     * Create a new pointer type with a specified target type and a
     * fixed width.
     *
     * @param target  type this pointer points to
     * @param width   width of the address, in bits
     */
    public TypePointer(Type target, int width)
    {
        this.target = target;
        this.width = width;
    }
    
    /**
     * Return the type this is a pointer to.  This can be viewed as a
     * <code>foo *</code>; this returns the type <code>foo</code>.
     * This function may also return <code>null</code> if it is an
     * abstract pointer to memory.
     *
     * @return  the type this points to, or null
     */
    public Type getTarget()
    {
        return target;
        // Pronounced "tar-zhay", with an accent.
    }

    /**
     * Returns the number of bits required to store this type.  This
     * is the bit width specified when the type object was
     * constructed.  The width may also be 0 to indicate "don't care".
     *
     * @return  the width of the type, in bits, or 0 for unknown
     */
    public int getBitWidth()
    {
        return width;
    }

    /**
     * Returns true if this can be converted to that without penalty.
     * This can be true only if that is a pointer type with the exact
     * same width as this, or the width of this is 0.  This also can
     * be true only if this's type is convertible to that's, or if
     * this's type is null.
     *
     * @param that  type to compare to
     * @return      true if this can be converted to that without penalty
     */
    public boolean isConvertibleTo(Type that)
    {
        if (!(that instanceof TypePointer))
            return false;
        TypePointer tp = (TypePointer)that;
        if (getBitWidth() != 0 && getBitWidth() != tp.getBitWidth())
            return false;
        if (getTarget() != null)
        {
            if (tp.getTarget() == null)
                return false;
            if (!(getTarget().isConvertibleTo(tp.getTarget())))
                return false;
        }
        return true;
    }
    
    /**
     * Returns true if this is equal to that.  This is true iff that
     * is an integer type with the same width as this.
     *
     * @param that  type to compare to
     * @return      true if this and that are the same type
     */
    public boolean equals(Object that)
    {
        if (!(that instanceof TypeInt))
            return false;
        if (((TypeInt)that).getBitWidth() != this.getBitWidth())
            return false;
        return true;
    }
    
    /**
     * Returns a hash code value for the object.  This uses the bit
     * width and signedness to produce a number that is equal for two
     * equal types.
     *
     * @return  a hash code value for this object
     */
    public int hashCode()
    {
        int val = getBitWidth() * 2;
        if (signed) val++;
        return val;
    }
}
