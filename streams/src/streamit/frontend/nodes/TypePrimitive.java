/*
 * TypePrimitive.java: a primitive data type
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: TypePrimitive.java,v 1.1 2002-07-15 15:44:08 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A primitive type.  This can be int, float, or complex, depending on
 * the specified type parameter.
 */
public class TypePrimitive extends Type
{
    public static final int TYPE_INT = 1;
    public static final int TYPE_FLOAT = 2;
    public static final int TYPE_COMPLEX = 3;
    
    private int type;

    public TypePrimitive(int type)
    {
        this.type = type;
    }
    
    public int getType()
    {
        return type;
    }
}
