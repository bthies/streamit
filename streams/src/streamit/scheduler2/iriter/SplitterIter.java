package streamit.scheduler2.iriter;

/* $Id: SplitterIter.java,v 1.4 2002-12-02 23:54:11 karczma Exp $ */

/**
 * An interface for retrieving data about streams with a Splitter.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface SplitterIter
{
    /**
     * Returns the number of ways this Splitter splits data.
     * @return return Splitter fan-out
     */
    public int getFanOut ();
    
    /**
     * Returns the number of work functions for this Splitter.
     * @return number of work functions for this Splitter
     */
    public int getSplitterNumWork ();
    
    /**
     * Returns n-th work function associated with this Splitter.
     * @return n-th work function for the Splitter
     */
    public Object getSplitterWork (int nWork);

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
