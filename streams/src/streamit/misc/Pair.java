package streamit.misc;

import streamit.misc.AssertedClass;

/* $Id: Pair.java,v 1.3 2003-07-21 15:47:36 jasperln Exp $ */

/**
 * <dl>
 * <dt> Purpose: Store two Objects in a Single Object
 * <dd>
 *
 * <dt>Description:
 * <dd> This class is used to store an ordered pair of objects in 
 * a single object.  The user can retrieve both objects and check
 * equality of pairs.  Objects stored can be null.
 * </dl>
 * 
 * @version 1
 * @author  Michal Karczmarek
 */

public class Pair extends AssertedClass
{
    Object first, second;

    /**
     * Constructor simply stores the two objects.
     * @return none
     */
    public Pair(Object _first, Object _second)
    {
        first = _first;
        second = _second;
    }

    /**
     * Returns a true if o is a Pair and if the two objects it stores
     * are the same handles.
     * @return true if same as o; false otherwise
     */
    public boolean equals(Object o)
    {
        // you can only compare two pairs!
        //ASSERT(o instanceof Pair);
	if(!(o instanceof Pair))
	    return false;

        Pair other = (Pair) o;

        int firstDiff, secondDiff;

        return getFirst() == other.getFirst() && 
                getSecond() == other.getSecond();
    }

    /**
     * Returns the first object of the pair.
     * @return first object of the pair
     */
    public Object getFirst()
    {
        return first;
    }

    /**
     * Returns the second object of the pair.
     * @return second object of the pair
     */
    public Object getSecond()
    {
        return second;
    }
}
