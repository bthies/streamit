/*
 * TypeArray.java: a fixed-length homogenous array type
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: TypeArray.java,v 1.1 2002-07-15 15:44:08 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A fixed-length homogenous array type.  This type has a base type and
 * an expression for the length.  The expression must be real and integral,
 * but may contain variables if they can be resolved by constant propagation.
 */
public class TypeArray extends Type
{
    private Type base;
    private Expression length;
    
    /** Creates an array type of the specified base type with the
     * specified length. */
    public TypeArray(Type base, Expression length)
    {
        this.base = base;
        this.length = length;
    }
    
    /** Gets the base type of this. */
    public Type getBase()
    {
        return base;
    }
    
    /** Gets the length of this. */
    public Expression getLength()
    {
        return length;
    }
}
