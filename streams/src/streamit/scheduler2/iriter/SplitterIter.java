package streamit.scheduler.iriter;

/* $Id: SplitterIter.java,v 1.1 2002-05-27 00:11:49 karczma Exp $ */

/**
 * An interface for retrieving data about streams with a Splitter.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

interface SplitterIter
{
    /**
     * Returns the number of work functions for the Splitter
     * of this Stream.
     * @return number of work functions for the Splitter of this 
     * Stream
     */
    public int getSplitterNumWork ();

    /**
     * Returns distribution of weights on a particular invocation
     * of work function for Splitter of this Stream.  The 
     * distribution is simply an array of ints, with numChildren 
     * elements.  The value at index 0 corresponds to number of items 
     * pushed out to 1st child, etc.
     * @return distribution of weights on a particular invocation
     * of work function for the Splitter of this Stream.
     */
    public int[] getSplitPushWeights (int nWork);
    
    /**
     * Returns number of data items consumed by a particular invocation
     * of work function for Splitter of this Stream.  These are the
     * items that will end up being pushed out to the children of this
     * Stream.
     * @return number of data items consumed by a particular invocation
     * of work function for Splitter of this Stream.
     */
    public int getSplitPop (int nWork);
}
