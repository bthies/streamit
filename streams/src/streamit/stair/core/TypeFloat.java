package streamit.stair.core;

/**
 * Type representing an floating-point number.
 *
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TypeFloat.java,v 1.1 2003-02-24 21:45:11 dmaze Exp $
 */
public class TypeFloat extends Type
{
    private int width;
    
    /**
     * Create a new floating-point type of known width.
     *
     * @param width   width of the integer, in bits
     */
    public TypeFloat(int width)
    {
        this.width = width;
    }

    /**
     * Create a new floating-point type of unknown width that can hold
     * a fixed value.
     */
    public TypeFloat()
    {
        this(0);
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
     * This is true iff that is a floating-point type that is at least
     * as long as this.
     *
     * @param that  type to compare to
     * @return      true if this can be converted to that without penalty
     */
    public boolean isConvertibleTo(Type that)
    {
        if (!(that instanceof TypeFloat))
            return false;
        if (that.getBitWidth() < this.getBitWidth())
            return false;
        return true;
    }
    
    /**
     * Returns true if this is equal to that.  This is true iff that
     * is a floating-point type with the same width as this.
     *
     * @param that  type to compare to
     * @return      true if this and that are the same type
     */
    public boolean equals(Object that)
    {
        if (!(that instanceof TypeFloat))
            return false;
        if (((TypeFloat)that).getBitWidth() != this.getBitWidth())
            return false;
        return true;
    }
    
    /**
     * Returns a hash code value for the object.  This uses the bit
     * width to produce a number that is equal for two equal types.
     *
     * @return  a hash code value for this object
     */
    public int hashCode()
    {
        return getBitWidth();
    }
}
