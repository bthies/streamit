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
