package streamit.stair.core;

/**
 * Type of a symbol, register, or other operand.  Types may have explicit
 * bit-widths, or may request the best-available compatible machine type.
 * There are primitive types ({@link TypeInt}, {@link TypeFloat}) and
 * composite types ({@link TypeStruct}, {@link TypeArray}).
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: Type.java,v 1.1 2003-02-24 21:45:11 dmaze Exp $
 */
public abstract class Type
{
    /**
     * Returns the number of bits required to store this type.
     * A variable of this width may be stored in a variable of
     * equal or greater width without losing information.  The
     * width may also be 0 to indicate "don't care"; this applies
     * to types with no fixed width in the source language, as
     * well as constants that don't come with an intrinsic width.
     *
     * @return  the width of the type, in bits, or 0 for unknown.
     */
    public abstract int getBitWidth();

    /**
     * Returns true if this can be converted to that without
     * penalty.  This generally happens if that is the same base
     * type as this, but is wider or longer.
     *
     * @param that  type to compare to
     * @return      true if this can be converted to that without penalty
     */
    public abstract boolean isConvertibleTo(Type that);
}
