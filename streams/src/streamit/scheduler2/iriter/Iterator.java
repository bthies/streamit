package streamit.scheduler2.iriter;

/* $Id: Iterator.java,v 1.5 2002-12-02 23:54:10 karczma Exp $ */

/**
 * <dl>
 * <dt>Purpose: An Iterator for StreamIt stream graph
 * <dd>
 *
 * <dt>Description:
 * <dd> This interface describes an iterator of a StreamIt stream graph.
 * All this iterator knows about is how to determine if its a 
 * Filter, Pipeline, SplitJoin or a FeedbackLoop.
 * Using iterators, the scheduler can construct its own internal
 * StreamIt graph.  This provides a good separation between StreamIt
 * compiler/library implementations and the scheduler.
 * </dl>
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface Iterator extends IteratorBase
{
    /**
     * Checks if the iterator points to a Filter.  If so, returns
     * a Filter iterator that points to the same object.  If not,
     * returns null.
     * @return iterator to a Filter or null if self doesn't point to 
     * a Filter
     */
    FilterIter isFilter();

    /**
     * Checks if the iterator points to a Pipeline.  If so, returns
     * a Pipeline iterator that points to the same object.  If not,
     * returns null.
     * @return iterator to a Pipeline or null if self doesn't point to
     * a Pipeline
     */
    PipelineIter isPipeline();
    
    /**
     * Checks if the iterator points to a SplitJoin.  If so, returns
     * a SplitJoin iterator that points to the same object.  If not,
     * returns null.
     * @return iterator to a SplitJoin or null if self doesn't point to
     * a SplitJoin
     */
    SplitJoinIter isSplitJoin();

    /**
     * Checks if the iterator points to a FeedbackLoop.  If so, returns
     * a FeedbackLoop iterator that points to the same object.  If not,
     * returns null.
     * @return iterator to a FeedbackLoop or null if self doesn't point
     * to a FeedbackLoop
     */
    FeedbackLoopIter isFeedbackLoop();
}