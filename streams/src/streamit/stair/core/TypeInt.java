package streamit.stair.core;

/**
 * Type representing an integer.
 *
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TypeInt.java,v 1.1 2003-02-24 21:45:11 dmaze Exp $
 */
public class TypeInt extends Type
{
    private int width;
    private boolean signed;
    
    /**
     * Create a new integer type of known width and signedness.
     *
     * @param width   width of the integer, in bits
     * @param signed  true if this is a signed integer
     */
    public TypeInt(int width, boolean signed)
    {
        this.width = width;
        this.signed = signed;
    }

    /**
     * Create a new integer type of unknown width that can hold
     * a particular value.  The type is signed if the value is
     * negative and unsigned otherwise.
     *
     * @param val  value to create a type to hold
     */
    public TypeInt(int val)
    {
        this(0, (val < 0));
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
     * Returns true if this is a signed integer, or false if this is
     * unsigned.
     *
     * @return  true if this is signed
     */
    public boolean isSigned()
    {
        return signed;
    }

    /**
     * Returns true if this can be converted to that without penalty.
     * This is true iff that is an integer type that is at least as
     * long as this.
     *
     * @param that  type to compare to
     * @return      true if this can be converted to that without penalty
     */
    public boolean isConvertibleTo(Type that)
    {
        if (!(that instanceof TypeInt))
            return false;
        if (that.getBitWidth() < this.getBitWidth())
            return false;
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
