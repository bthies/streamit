package streamit.frontend.nodes;

/**
 * A primitive type.  This can be int, float, or complex, depending on
 * the specified type parameter.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TypePrimitive.java,v 1.5 2003-06-30 20:23:12 dmaze Exp $
 */
public class TypePrimitive extends Type
{
    public static final int TYPE_BIT = 1;
    public static final int TYPE_INT = 2;
    public static final int TYPE_FLOAT = 3;
    public static final int TYPE_DOUBLE = 4;
    public static final int TYPE_COMPLEX = 5;
    public static final int TYPE_VOID = 6;
    public static final int TYPE_BOOLEAN = 7;
    
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

    public String toString()
    {
        switch (type)
        {
        case TYPE_BIT:
            return "bit";
        case TYPE_INT:
            return "int";
        case TYPE_FLOAT:
            return "float";
        case TYPE_DOUBLE:
            return "double";
        case TYPE_COMPLEX:
            return "complex";
        case TYPE_VOID:
            return "void";
        case TYPE_BOOLEAN:
            return "boolean";
        default:
            return "<primitive type " + type + ">";
        }
    }
    
    public boolean equals(Object other)
    {
        // Two cases.  One, this is complex, and so is that:
        if (other instanceof Type)
        {
            Type that = (Type)other;
            if (this.isComplex() && that.isComplex())
                return true;
        }
        // Two, these are both primitive types with the same type code.
        if (!(other instanceof TypePrimitive))
            return false;
        TypePrimitive that = (TypePrimitive)other;
        if (this.type != that.type)
            return false;
        return true;
    }
    
    public int hashCode()
    {
        return new Integer(type).hashCode();
    }
}
