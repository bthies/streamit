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
 * ListIterator is the Java equivalent of STL list::iterator.
 * For usage information, check STL documentation
 * 
 */

public class DLListIterator
{
    DLListElement element;
    
    /**
     * The only constructor.  It's protected from the
     * outside world, as only a List can create a
     * constructor.  If you want to generate a new one
     * yourself, you have to clone it from an existing
     * iterator using copy() method.
     */    
    DLListIterator (DLListElement element)
    {
        // check that the element is actually valid
        assert element != null;
        
        // initiate element
        this.element = element;
    }
    
    /**
     * Advance the iterator to the next element
     */
    public void next ()
    {
        // make sure that I'm not on the "root" element.
        // the "root" element has no next - it's treated as the "last"
        // element
        assert element.data != element;
        element = element.next ();
    }
    
    /**
     * Advance the iterator to the previous element
     */
    public void prev ()
    {
        element = element.prev ();
        
        // make sure that I'm not on the "root" element.
        // the "root" element indicates I've wrapped around the list
        assert element.data != element;
    }
    
    /**
     * return the data pointed to by the iterator
     */
    public Object get ()
    {
        return element.get ();
    }
    
    /**
     * make a copy of this iterator and return it
     */
    public DLListIterator copy ()
    {
        return new DLListIterator (element);
    }
    
    /**
     * check if this iterator points to same data as some other iterator.
     */
    public boolean equals (DLListIterator other)
    {
        return element == other.element;
    }
    
    /**
     * get the DLListElement of this DLListIterator
     */
    DLListElement getListElement ()
    {
        return element;
    }
}
