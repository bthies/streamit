package streamit.scheduler.iriter;

/* $Id: JoinerIter.java,v 1.2 2002-05-27 02:56:39 karczma Exp $ */

/**
 * An interface for retrieving data about streams with a Joiner.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface JoinerIter
{
    /**
     * Returns the number of ways this Joiner joines data.
     * @return return Joiner fan-in
     */
    public int getFanIn ();
    
    /**
     * Returns the number of work functions for the Joiner
     * of this Stream.
     * @return number of work functions for the JOiner of this 
     * Stream
     */
    public int getJoinerNumWork ();

    /**
     * Returns distribution of weights on a particular invocation
     * of work function for Joiner of this SplitJoin.  The 
     * distribution is simply an array of ints, with numChildren 
     * elements.  The value at index 0 corresponds to number of items 
     * popped from 1st child, etc.
     * @return distribution of weights on a particular invocation
     * of work function for Joiner of this SplitJoin.
     */
    public int[] getJoinPopWeights (int nWork);
    
    /**
     * Returns number of data items produced by a particular invocation
     * of work function for Joiner of this SplitJoin.  These are the
     * items that were consumed from various children of this SplitJoin.
     * @return number of data items produced by a particular invocation
     * of work function for Joiner of this SplitJoin.
     */
    public int getJoinPush (int nWork);
}