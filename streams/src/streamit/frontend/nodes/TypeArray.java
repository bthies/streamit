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
 * A fixed-length homogenous array type.  This type has a base type and
 * an expression for the length.  The expression must be real and integral,
 * but may contain variables if they can be resolved by constant propagation.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TypeArray.java,v 1.4 2006-03-16 21:16:51 madrake Exp $
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

    /** gets the component that this array is made out of */
    public Type getComponent() 
    {
        Type component = this;
        while (component instanceof TypeArray)
            component = ((TypeArray) component).getBase();
        return component;      
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
