package streamit.scheduler.iriter;

/* $Id: SplitterNJoinerIter.java,v 1.2 2002-12-02 20:40:01 karczma Exp $ */

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