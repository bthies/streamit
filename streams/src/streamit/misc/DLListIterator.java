package streamit.misc;

/**
 * 
 * ListIterator is the Java equivalent of STL list::iterator.
 * For usage information, check STL documentation
 * 
 */

public class DLListIterator extends AssertedClass
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
        ASSERT (element != null);
        
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
        ASSERT (element.data != element);
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
        ASSERT (element.data != element);
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
}