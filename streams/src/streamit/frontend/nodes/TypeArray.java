package streamit.frontend.nodes;

/**
 * A fixed-length homogenous array type.  This type has a base type and
 * an expression for the length.  The expression must be real and integral,
 * but may contain variables if they can be resolved by constant propagation.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TypeArray.java,v 1.2 2003-07-15 18:55:54 dmaze Exp $
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

    public String toString()
    {
        return base + "[" + length + "]";
    }
    
    public boolean equals(Object other)
    {
        if (!(other instanceof TypeArray))
            return false;
        TypeArray that = (TypeArray)other;
        if (!(this.getBase().equals(that.getBase())))
            return false;
        if (!(this.getLength().equals(that.getLength())))
            return false;
        return true;
    }
    
    public int hashCode()
    {
        return base.hashCode() ^ length.hashCode();
    }
}
