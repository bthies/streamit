/*
 * Copyright 2006 by the Massachusetts Institute of Technology.
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

package streamit.library;

import java.util.*;
import java.lang.reflect.Array;

class ArrayInitArguments
{
    private Class componentType;
    private int[] dimensions;
    public ArrayInitArguments(Class _componentType, int[] _dimensions) 
    {
        componentType = _componentType;
        dimensions = _dimensions;
    }
        public boolean equals(Object o) 
    {
        if (o instanceof ArrayInitArguments)
            {
                ArrayInitArguments other = (ArrayInitArguments) o;
                    return (other.componentType.equals(this.componentType) &&
                            Arrays.equals(this.dimensions, other.dimensions));
                    
            }
        else
            return false;
    }
    public int hashCode() {
        return dimensions.hashCode() * componentType.hashCode();
    }
}

/**
 * An ArrayMemoizer is a singleton that, given a type and a set of dimensions,
 * returns an initialized copy of the array. For a given type and dimensions it
 * always returns the same copy of the initialized array, saving memory.
 * This is only to be used so that immutable arrays which are accessed 
 * before assignment maintain the correct language semantics (rather than crashing
 * the program).
 */
public class ArrayMemoizer 
{
    // The single instance of this class that exists.
    private static ArrayMemoizer singleton = new ArrayMemoizer();

    private ArrayMemoizer() {};

    private Map<ArrayInitArguments, Object> storedArrays = new HashMap<ArrayInitArguments, Object>();

    /**
     * Returns the initial copy of an array with a given type and dimensions.
     * @param sampleArrayOfType An array of the same base type
     * @param dimensions the dimensions of the array
     * @return A reference to an initialized array with the correct type and dimensions.
     *         Always returns the same copy 
     */
    // the fact that this takes in a small sample array rather than a class is a 
    // workaround to the fact that there's not an easy way in the IR to specify
    // a class.
    public static Object initArray(Object sampleArrayOfType, int[] dimensions) 
        throws NegativeArraySizeException
    {
        Class componentType = sampleArrayOfType.getClass().getComponentType();
        ArrayInitArguments args = new ArrayInitArguments(componentType, dimensions);
        Object lookupArray = singleton.storedArrays.get(args);
        if (lookupArray == null)
            {
                lookupArray = Array.newInstance(componentType,
                                                dimensions);
                singleton.storedArrays.put(args, lookupArray);
            } 
        return lookupArray;  
    }
   
}







