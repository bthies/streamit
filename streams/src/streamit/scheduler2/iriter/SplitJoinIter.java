package streamit.scheduler.iriter;

import streamit.scheduler.iriter.Iterator;

/* $Id: SplitJoinIter.java,v 1.4 2002-05-24 23:10:33 karczma Exp $ */

/**
 * <dl>
 * <dt>Purpose: An Iterator for StreamIt Graph
 * <dd>
 *
 * <dt>Description:
 * <dd> This interface describes an iterator for a SplitJoin.
 * </dl>
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface SplitJoinIter
{
    public class SplitJoinWeights extends streamit.misc.DestroyedClass
    {
        int sjRate;
        int [] childrenRate;
    }
    
    /**
     * Returns the number of children that this SplitJoin has.
     * If some of the children and sinks or joins, they still
     * count to this total.
     * @return number of children of this SplitJoin
     */
    public int getNumChildren ();

    /**
     * Returns the n-th child of the SplitJoin.
     * If the SplitJoin doesn't have enough children (n is too large),
     * this function ASSERTS and throws a RuntimeException.
     * @return n-th child of the SplitJoin
     */
    public Iterator getChild (int n);
    
    /**
     * enum value for an invalid splitter/joiner
     */
    public final int SJ_ERROR = -1;
    
    /**
     * enum value for a NULL splitter/joiner
     */
    public final int SJ_NULL = 0;
    
    /**
     * enum value for a weighted or unweighted round robin
     * splitter/joiner
     */
    public final int SJ_WEIGHTED_ROUND_ROBIN = 1;
    
    /**
     * enum value for a duplicate splitter
     */
    public final int SJ_DUPLICATE = 2;
    
    /**
     * enum value for number of types of splits/joins.  May increase
     * as we add more and more split/join types (most notably, custom
     * and variable rates)
     */
    public final int NUM_SJ_TYPES = 3;
    
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
