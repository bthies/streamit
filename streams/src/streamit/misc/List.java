package streamit.misc;

/**
 * 
 * List is a Java equivalent of the STL list class.
 * For usage information, check STL documentation
 * 
 */

public class List
{
    /**
     * This is the root of the list.  This element
     * points to both the first and the last elements
     * of the actual list.
     */
    ListElement theList = new ListElement ();
    

    /**
     * Returns the first element of a list. 
     */    
    public ListIterator front ()
    {
        return new ListIterator (theList.nextElem);
    }
    
    /**
     * Returns the last element of a list.
     * The last element of a list is not actually a valid element, it is the
     * element beyond the last element.
     */
    public ListIterator back ()
    {
        return new ListIterator (theList);
    }
    
    /**
     * Returns true iff the list has no elements in it
     */
    public boolean empty ()
    {
        return theList.nextElem == theList;
    }
}
