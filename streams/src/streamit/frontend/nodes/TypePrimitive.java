/*
 * TypePrimitive.java: a primitive data type
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: TypePrimitive.java,v 1.3 2002-08-16 18:39:01 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A primitive type.  This can be int, float, or complex, depending on
 * the specified type parameter.
 */
public class TypePrimitive extends Type
{
    public static final int TYPE_BIT = 1;
    public static final int TYPE_INT = 2;
    public static final int TYPE_FLOAT = 3;
    public static final int TYPE_DOUBLE = 4;
    public static final int TYPE_COMPLEX = 5;
    public static final int TYPE_VOID = 6;
    
    private int type;

    public TypePrimitive(int type)
    {
        this.type = type;
    }
    
    public int getType()
    {
        return type;
    }

    public boolean isComplex()
    {
        return type == TYPE_COMPLEX;
    }
}
