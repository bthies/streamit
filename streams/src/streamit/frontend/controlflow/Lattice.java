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
 * An element in a well-formed lattice structure.  Lattices have
 * distinguished elements 'top' and 'bottom', and commutative
 * operations 'meet' and 'join'; of note, (anything meet bottom) is
 * bottom, and (anything join top) is top.  Lattices are used
 * in data-flow analysis.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: Lattice.java,v 1.1 2004-01-21 21:04:31 dmaze Exp $
 */
public interface Lattice
{
    /**
     * Get the 'top' element of the lattice.  This is a distinguished
     * element such that (x join top) is top, and (x meet top) is x,
     * for all x in the lattice.
     *
     * @return  the top element of the lattice
     */
    public Lattice getTop();

    /**
     * Get the 'bottom' element of the lattice.  This is a distinguished
     * element such that (x meet bottom) is bottom, and (x join bottom)
     * is x, for all x in the lattice.  Given an operator < on lattice
     * elements, either bottom == x or bottom < x for all x.
     *
     * @return  the bottom element of the lattice
     */
    public Lattice getBottom();

    /**
     * Find the 'meet' of two elements.  This can be thought of as
     * the greatest lower bound of two elements: given an operator <
     * on lattice elements, (x meet y) is the largest element z
     * such that z < x and z < y, even if x and y aren't comparable.
     *
     * @return (this meet other)
     */
    public Lattice meet(Lattice other);

    /**
     * Find the 'join' of two elements.  This can be thought of as
     * the least upper bound of two elements: given an operator <
     * on lattice elements, (x join y) is the smallest element z
     * such that x < z and y < z, even if x and y aren't comparable.
     *
     * @return (this join other)
     */
    // public Lattice join(Lattice other);
    // unused by data-flow analysis
}
