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
 * List is a Java equivalent of the STL list class.
 * For usage information, check STL documentation
 * 
 */

public class DLList extends AssertedClass implements DLList_const
{
    /**
     * This is the root of the list.  This element
     * points to both the first and the last elements
     * of the actual list.
     */
    DLListElement theList = new DLListElement();

    /**
     * Make a copy of the list. This does NOT copy the elements!
     */
    public DLList copy()
    {
        DLList other = new DLList();

        for (DLListElement elem = theList.nextElem;
            elem != theList;
            elem = elem.nextElem)
        {
            other.pushBack(elem.get());
        }

        return other;
    }

    /**
     * Returns the first element of a list. 
     */
    public DLListIterator front()
    {
        return new DLListIterator(theList.nextElem);
    }

    /**
     * Returns the last element of a list.
     * The last element of a list is not actually a valid element, it is the
     * element beyond the last element.
     */
    public DLListIterator back()
    {
        return new DLListIterator(theList);
    }

    public DLListIterator begin()
    {
        return new DLListIterator(theList.nextElem);
    }

    public DLListIterator end()
    {
        return new DLListIterator(theList);
    }

    public void pushBack(Object data)
    {
        new DLListElement(theList.prevElem, data);
    }

    public void pushFront(Object data)
    {
        new DLListElement(theList, data);
    }

    public void popFront()
    {
        DLListElement toRemove = theList.nextElem;

        // make sure that there is an element to pop
        ASSERT(toRemove != theList);

        removeElement(toRemove);
    }

    public void popBack()
    {
        DLListElement toRemove = theList.prevElem;

        // make sure that there is an element to pop
        ASSERT(toRemove != theList);

        removeElement(toRemove);
    }
    
    /**
     * Remove an entry pointed to by pos, and return an iterator
     * pointing to the next element.
     * 
     * This function will ADVANCE pos! This is OK, because after
     * the execution of this function, pos should be invalid, anyway. 
     */
    public DLListIterator erase (DLListIterator pos)
    {
        DLListElement listElement = pos.getListElement();
        
        // make sure I'm not removing the root element!
        ASSERT(listElement != theList);
        
        pos.next ();
        removeElement (listElement);
        return pos;
    }

    private void removeElement(DLListElement elem)
    {
        // make sure I'm not removing the root element!
        ASSERT(elem != theList);

        // and that the element hasn't been removed yet!
        ASSERT(elem.nextElem != null);

        // now remove the element
        DLListElement prevElem = elem.prevElem;
        DLListElement nextElem = elem.nextElem;
        prevElem.nextElem = nextElem;
        nextElem.prevElem = prevElem;

        // and reset the element's prev and next fields
        elem.prevElem = null;
        elem.nextElem = null;
    }

    /**
     * Returns true iff the list has no elements in it
     */
    public boolean empty()
    {
        return theList.nextElem == theList;
    }
}
