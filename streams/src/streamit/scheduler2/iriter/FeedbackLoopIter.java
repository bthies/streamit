package streamit.scheduler2.iriter;

/* $Id: FeedbackLoopIter.java,v 1.9 2002-12-02 23:54:10 karczma Exp $ */

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
     * Returns an iterator for the body of the FeedbackLoop.
     * @return iterator for the body of the FeedbackLoop
     */
    public Iterator getBodyChild ();

    /**
     * Returns an iterator for the loop of the FeedbackLoop.
     * @return iterator for the loop of the FeedbackLoop
     */
    public Iterator getLoopChild ();
    
    /**
     * Returns the delay size for this feedback loop.
     * @return delay size
     */
    public int getDelaySize();
}
