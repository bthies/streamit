/*
 * Type.java: base class for variable data types
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: Type.java,v 1.2 2002-07-15 18:50:58 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * Base class for variable data types.
 */
public abstract class Type
{
    /** Returns true if this type is a complex type. */
    public boolean isComplex() { return false; }
}
