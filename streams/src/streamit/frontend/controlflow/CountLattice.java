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

/**
 * Simple lattice that counts something.  The value of the lattice can
 * be an integer specifying the count, or top (nothing determines the
 * count) or bottom (conflicting count values).
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: CountLattice.java,v 1.1 2004-01-21 21:13:00 dmaze Exp $
 */
public class CountLattice implements Lattice
{
    private boolean top, bottom;
    private int value;
    
    /**
     * Create a lattice element with some value.
     *
     * @param n  value of the lattice element
     */
    public CountLattice(int n)
    {
        top = false;
        bottom = false;
        value = n;
    }
    
    /**
     * Create a top or bottom lattice element (without a value).
     *
     * @param isTop  true for top; false for bottom
     */
    public CountLattice(boolean isTop)
    {
        top = isTop;
        bottom = !isTop;
        value = 0; // arbitrary
    }
    
    public Lattice getTop() 
    {
        return new CountLattice(true);
    }
    
    public Lattice getBottom()
    {
        return new CountLattice(false);
    }
    
    public Lattice meet(Lattice other)
    {
        // ASSERT: other is a CountLattice
        CountLattice that = (CountLattice)other;

        // If either is bottom, return bottom:
        if (this.bottom || that.bottom)
            return new CountLattice(false);
        
        // If either is top, return the other:
        if (this.top)
            return that;
        if (that.top)
            return this;
        
        // If the values match, they're the same element, so return it:
        if (this.value == that.value)
            return this;
        
        // If the values don't match, meet is bottom.
        return new CountLattice(false);
    }
    
    // punt on join, it's optional

    public boolean isTop()
    {
        return top;
    }
    
    public boolean isBottom()
    {
        return bottom;
    }
    
    public int getValue()
    {
        return value;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof CountLattice))
            return false;
        CountLattice that = (CountLattice)other;
        if (this.top != that.top)
            return false;
        if (this.bottom != that.bottom)
            return false;
        if (this.value != that.value)
            return false;
        return true;
    }
}
