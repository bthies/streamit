package streamit.scheduler.iriter;

/* $Id: Iterator.java,v 1.2 2002-05-01 23:20:04 karczma Exp $ */

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

public interface Iterator
{
    FilterIter isFilter ();
    PipelineIter isPipeline ();
    SplitJoinIter isSplitJoin ();
    FeedbackLoopIter isFeedbackLoop ();
}
