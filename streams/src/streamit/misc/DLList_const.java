package streamit.misc;

/**
 * 
 * List is a Java equivalent of the STL const list class.
 * For usage information, check STL documentation
 * 
 */

public interface DLList_const
{
    /**
     * Make a copy of the list. This does NOT copy the elements!
     */
    public DLList copy();

    /**
     * Returns the first element of a list. 
     */
    public DLListIterator front();

    public DLListIterator begin();

    /**
     * Returns the last element of a list.
     * The last element of a list is not actually a valid element, it is the
     * element beyond the last element.
     */
    public DLListIterator end();

    public DLListIterator back();

    /**
     * Returns true iff the list has no elements in it
     */
    public boolean empty();
}
