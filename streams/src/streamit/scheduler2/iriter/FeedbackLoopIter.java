package streamit.scheduler.iriter;

import streamit.scheduler.iriter.Iterator;

/* $Id: FeedbackLoopIter.java,v 1.3 2002-05-22 00:28:19 karczma Exp $ */

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

public interface FeedbackLoopIter
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
    
    /**
     * Returns an appropriate value for every possible splitter.
     * If the splitter is not recognized, returns SJ_ERROR.
     * @return enum value of the splitter type
     */
    public int getSplitterType ();

    /**
     * Returns the number of work functions for the Splitter
     * of this SplitJoin.
     * @return number of work functions for the Splitter of this 
     * SplitJoin
     */
    public int getNumWorkSplitter ();

    /**
     * Returns distribution of weights on a particular invocation
     * of work function for Splitter of this SplitJoin.  The 
     * distribution is simply an array of ints, with numChildren + 1 
     * elements.  The value at index 0 corresponds to number of items 
     * consumed by the Splitter.  The remaining elements each
     * correspond to amount of data pushed out to the corresponding
     * child.
     * @return distribution of weights on a particular invocation
     * of work function for the Splitter of this SplitJoin.
     */
    public int[] getSplitWeights (int n);
    
    /**
     * Returns an appropriate value for every possible joiner.
     * If the joiner is not recognized, returns SJ_ERROR.
     * @return enum value of the joiner type
     */
    public int getJoinerType ();
    
    /**
     * Returns the number of work functions for the Joiner
     * of this SplitJoin.
     * @return number of work functions for the JOiner of this 
     * SplitJoin
     */
    public int getNumWorkJoiner ();

    /**
     * Returns distribution of weights on a particular invocation
     * of work function for Joiner of this SplitJoin.  The 
     * distribution is simply an array of ints, with numChildren + 1 
     * elements.  The value at index 0 corresponds to number of items 
     * produced by the SplitJoin.  The remaining elements each
     * correspond to the amount of data consumed from the corresponding
     * child.
     * @return distribution of weights on a particular invocation
     * of work function for the Joiner of this SplitJoin.
     */
    public int[] getJoinWeights (int n);
}
