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
 * Type-tracking lattice that finds any compatible type.
 * When comparing types at meets or joins, if the types disagree,
 * find some common type that both types can satisfy.  For StreamIt,
 * then, boolean would be nearest top, and complex nearest bottom.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TypeLattice.java,v 1.1 2004-01-26 19:33:36 dmaze Exp $
 */
public class TypeLattice extends AnyTypeLattice
{
    public TypeLattice(Type t) { super(t); }
    public TypeLattice(boolean isTop) { super(isTop); }

    public Lattice getTop() 
    {
        return new TypeLattice(true);
    }
    
    public Lattice getBottom()
    {
        return new TypeLattice(false);
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
        
        // Try to find the least common type, then.
        Type common = this.value.leastCommonPromotion(that.value);
        if (common != null)
            return new TypeLattice(common);
        
        // We lose, meet is bottom.
        return getBottom();
    }
}
