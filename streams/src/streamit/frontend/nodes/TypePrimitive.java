/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.frontend.nodes;

/**
 * A primitive type.  This can be int, float, or complex, depending on
 * the specified type parameter.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TypePrimitive.java,v 1.7 2003-12-18 18:57:29 dmaze Exp $
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
    
    /**
     * Check if this type can be promoted to some other type.
     * Returns true if a value of this type can be assigned to
     * a variable of that type.  For primitive types, promotions
     * are ordered: boolean -> bit -> int -> float -> complex.
     *
     * @param that  other type to check promotion to
     * @return      true if this can be promoted to that
     */
    public boolean promotesTo(Type that)
    {
        if (super.promotesTo(that))
            return true;
        if (!(that instanceof TypePrimitive))
            return false;

        int t1 = this.type;
        int t2 = ((TypePrimitive)that).type;
        
        // want: "t1 < t2", more or less
        switch(t1)
        {
        case TYPE_BOOLEAN:
            return t2 == TYPE_BOOLEAN || t2 == TYPE_BIT ||
                t2 == TYPE_INT || t2 == TYPE_FLOAT ||
                t2 == TYPE_COMPLEX;
        case TYPE_BIT:
            return t2 == TYPE_BIT || t2 == TYPE_INT ||
                t2 == TYPE_FLOAT || t2 == TYPE_COMPLEX;
        case TYPE_INT:
            return t2 == TYPE_INT || t2 == TYPE_FLOAT || t2 == TYPE_COMPLEX;
        case TYPE_FLOAT:
            return t2 == TYPE_FLOAT || t2 == TYPE_COMPLEX;
        case TYPE_COMPLEX:
            return t2 == TYPE_COMPLEX;
        default:
            return false;
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
