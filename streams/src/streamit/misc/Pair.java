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

package streamit.misc;

/* $Id: Pair.java,v 1.8 2006-10-02 17:53:20 dimock Exp $ */

/**
 * Purpose: Store two Objects in a Single Object.
 *
 * <dt>Description:
 * <dd> This class is used to store an ordered pair of objects in 
 * a single object.  The user can retrieve both objects and check
 * equality of pairs.  Objects stored can be null.
 * </dl>
 * 
 * @author  Michal Karczmarek
 */

public class Pair<S,T>
{
    S first; 
    T second;

    /**
     * Constructor simply stores the two objects.
     * @param first : retrievable by getFirst.
     * @param second : retrievable by getSecond.
     */
    public Pair(S first, T second)
    {
        this.first = first;
        this.second = second;
    }

    /**
     * Returns a true if o is a Pair and if the two objects it stores
     * are the same handles.
     * @return true if same as o; false otherwise
     */
    public boolean equals(Object o)
    {
        // you can only compare two pairs!
        //assert o instanceof Pair;
        if(!(o instanceof Pair))
            return false;

        Pair other = (Pair) o;

        return getFirst() == other.getFirst() && 
            getSecond() == other.getSecond();
    }

    /**
     * Returns the first object of the pair.
     * @return first object of the pair
     */
    public S getFirst()
    {
        return first;
    }

    /**
     * Returns the second object of the pair.
     * @return second object of the pair
     */
    public T getSecond()
    {
        return second;
    }
}
