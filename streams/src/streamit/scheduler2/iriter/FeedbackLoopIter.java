package streamit.scheduler.iriter;

import streamit.scheduler.iriter.Iterator;

/* $Id: FeedbackLoopIter.java,v 1.5 2002-05-27 00:11:48 karczma Exp $ */

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

public interface FeedbackLoopIter extends IteratorBase, SplitterIter, JoinerIter
{
    /**
     * Returns an iterator for the body of the FeedbackLoop.
     * @return iterator for the body of the FeedbackLoop
     */
    public Iterator getBody ();

    /**
     * Returns an iterator for the loop of the FeedbackLoop.
     * @return iterator for the loop of the FeedbackLoop
     */
    public Iterator getLoop ();
}
