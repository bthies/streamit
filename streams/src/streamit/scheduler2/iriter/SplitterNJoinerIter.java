package streamit.scheduler2.iriter;

/**
 * An interface for retrieving data about streams with a Splitter and
 * a Joiner.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface SplitterNJoinerIter extends SplitterIter, JoinerIter
{
    /**
     * Returns an Iterator that pointst to the same object as this 
     * specialized iterator.
     * @return an Iterator that points to the same object
     */
    public Iterator getUnspecializedIter();
}