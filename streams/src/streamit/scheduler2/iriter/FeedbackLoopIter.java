package streamit.scheduler.iriter;

/* $Id: FeedbackLoopIter.java,v 1.6 2002-06-30 04:01:13 karczma Exp $ */

/**
 * <dl>
 * <dt>Purpose: An Iterator for StreamIt Graph
 * <dd>
 *
 * <dt>Description:
 * <dd> This interface describes an iterator for a FeedbackLoop.
 * </dl>
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface FeedbackLoopIter extends IteratorBase, SplitterNJoinerIter
{
    /**
     * Returns an Iterator that pointst to the same object as this 
     * specialized iterator.
     * @return an Iterator that points to the same object
     */
    public Iterator getUnspecializedIter();
    
    /**
     * Returns an iterator for the body of the FeedbackLoop.
     * @return iterator for the body of the FeedbackLoop
     */
    public Iterator getBodyChild ();

    /**
     * Returns an iterator for the loop of the FeedbackLoop.
     * @return iterator for the loop of the FeedbackLoop
     */
    public Iterator getLoopChild ();
}
