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

/**
 * 
 * ListElement is used to keep track of elements of a List.
 * Every list has a single element, which is the root of
 * the list.  Root's nextElem points to the first element in
 * the list, and prevElem points to the last element in the
 * list.  It can be easily distinguished from all other
 * elements because its data member points to itself.
 * 
 */

public class DLListElement extends AssertedClass
{
    final Object data;
    
    DLListElement prevElem;
    DLListElement nextElem;
    
    /**
     * Standard constructor.  Requires all the data.
     */
    DLListElement (DLListElement prev, Object data)
    {
        // check that both prev and next are valid
        ASSERT (prev != null);
        
        // instantiate the element
        prevElem = prev;
        nextElem = prev.nextElem;
        this.data = data;
        
        // and insert the element into the actual list
        nextElem.prevElem = this;
        prevElem.nextElem = this;
    }
    
    /**
     * Special constructor for constructing an element 
     * that loops into itself.  This element is used by 
     * the List class to generate a first/last link into 
     * the list.  This is the only ListElement that has
     * data == this - that's how it is distinguished from
     * all other elements.
     */
    DLListElement ()
    {
        prevElem = this;
        nextElem = this;
        data = this;
    }
    
    /**
     * Return the next element in the list.
     * Checks that the current element is not the root
     * of the list (the root does not have a next element!)
     */
    public DLListElement next ()
    {
        // check that this is not the root
        ASSERT (data != this);
        
        // and that there exists a next element
        // if there is no next element, then the
        // list is corrupt or the element has been
        // removed from the list already!
        ASSERT (nextElem != null);
    
    	// and return the nextElement    
        return nextElem;
    }
    
    /**
     * Return the prev element in the list.
     * Checks that the prev element is not the root
     * of the list (root element is not a prev element
     * for any element)
     */
    public DLListElement prev ()
    {
        // check that the prev element exists.
        // if it doesn't, then the list is corrupt
        // or the element has been removed from 
        // the list already!
        ASSERT (prevElem != null);
        
        // make sure that the previous element is 
        // not the root element
        ASSERT (prevElem.data != prevElem);
        
        // and return the previous element
        return prevElem;
    }
    
    /**
     * Return the data belonging to this element.
     * Checks that this is not the root element.
     */
    public Object get ()
    {
        // make sure that this is not the root
        // (which holds no data)
        ASSERT (data != this);
     
     	// return the data   
        return data;
    }
}
