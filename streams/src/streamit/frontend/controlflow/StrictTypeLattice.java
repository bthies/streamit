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
 * Type-tracking lattice that requires that types match exactly.
 * When comparing types at meets or joins, if the types are not
 * .equals() to each other, return top or bottom as appropriate.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StrictTypeLattice.java,v 1.1 2004-01-26 19:33:36 dmaze Exp $
 */
public class StrictTypeLattice extends AnyTypeLattice
{
    public StrictTypeLattice(Type t) { super(t); }
    public StrictTypeLattice(boolean isTop) { super(isTop); }

    public Lattice getTop() 
    {
        return new StrictTypeLattice(true);
    }
    
    public Lattice getBottom()
    {
        return new StrictTypeLattice(false);
    }
    
    public Lattice meet(Lattice other)
    {
        AnyTypeLattice that = (AnyTypeLattice)other;

        // If either is bottom, return bottom:
        if (this.bottom || that.bottom)
            return getBottom();
        
        // If either is top, return the other:
        if (this.top)
            return that;
        if (that.top)
            return this;
        
        // If the values match, they're the same element, so return it:
        if (this.value.equals(that.value))
            return this;
        
        // If the values don't match, meet is bottom.
        return getBottom();
    }
}
