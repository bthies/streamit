package streamit.misc;

/*
 * (c) Copyright Michal Karczmarek, 2002
 */

/**
 * 
 * ListIterator is the Java equivalent of STL list::iterator.
 * For usage information, check STL documentation
 * 
 */

public class ListIterator extends AssertedClass
{
    ListElement element;
    
    /**
     * The only constructor.  It's protected from the
     * outside world, as only a List can create a
     * constructor.  If you want to generate a new one
     * yourself, you have to clone it from an existing
     * iterator using copy() method.
     */    
    ListIterator (ListElement element)
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
        element = element.next ();
    }
    
    /**
     * Advance the iterator to the previous element
     */
    public void prev ()
    {
        element = element.prev ();
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
    ListIterator copy ()
    {
        return new ListIterator (element);
    }
}