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
 * Base class for variable data types.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: Type.java,v 1.4 2003-12-18 18:34:30 dmaze Exp $
 */
public abstract class Type
{
    /** Returns true if this type is a complex type. */
    public boolean isComplex() { return false; }

    /**
     * Check if this type can be promoted to some other type.
     * Returns true if a value of this type can be assigned to
     * a variable of that type.
     *
     * @param that  other type to check promotion to
     * @return      true if this can be promoted to that
     */
    public boolean promotesTo(Type that)
    {
        if (this.equals(that))
            return true;
        return false;
    }

    /**
     * Find the lowest type that two types can promote to.
     *
     * @param that  other type
     * @return      a type such that both this and that can promote
     *              to type, or null if there is no such type
     */
    public Type leastCommonPromotion(Type that)
    {
        if (this.promotesTo(that))
            return that;
        if (that.promotesTo(this))
            return this;
        return null;
    }
}
