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

package streamit.frontend.controlflow;

import streamit.frontend.nodes.Type;

/**
 * Base class for lattices that track a type.  The value of the lattice
 * can be top (no information), bottom (conflicting types), or a
 * StreamIt {@link streamit.frontend.nodes.Type} value; the interaction
 * of these is defined by the derived class.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: AnyTypeLattice.java,v 1.1 2004-01-26 19:33:36 dmaze Exp $
 */
public abstract class AnyTypeLattice implements Lattice
{
    protected boolean top, bottom;
    protected Type value;
    
    /**
     * Create a lattice element with some value.
     *
     * @param t  value of the lattice element
     */
    public AnyTypeLattice(Type t)
    {
        top = false;
        bottom = false;
        value = t;
    }
    
    /**
     * Create a top or bottom lattice element (without a value).
     *
     * @param isTop  true for top; false for bottom
     */
    public AnyTypeLattice(boolean isTop)
    {
        top = isTop;
        bottom = !isTop;
        value = null;
    }
    
    public boolean isTop()
    {
        return top;
    }
    
    public boolean isBottom()
    {
        return bottom;
    }
    
    public Type getValue()
    {
        return value;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof AnyTypeLattice))
            return false;
        AnyTypeLattice that = (AnyTypeLattice)other;
        if (this.top != that.top)
            return false;
        if (this.bottom != that.bottom)
            return false;
        if (this.value == null && that.value != null)
            return false;
        if (this.value != null && that.value == null)
            return false;
        if (this.value != null && !(this.value.equals(that.value)))
            return false;
        return true;
    }
}
