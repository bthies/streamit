package at.dms.kjc.iterator; 

import at.dms.kjc.sir.*;
import at.dms.util.*;
import streamit.scheduler2.iriter.*;

public class SIRFineGrainedFeedbackLoopIter extends SIRFeedbackLoopIter {

    /**
     * Returns new iterator for <obj> with no parent.
     */
    SIRFineGrainedFeedbackLoopIter(IterFactory _factory, SIRFeedbackLoop obj) {
	super(_factory, obj);
    }

    /**
     * Returns new iterator for <obj> in position <pos> of parent <parent>.
     */
    SIRFineGrainedFeedbackLoopIter(IterFactory _factory, SIRFeedbackLoop obj, SIRIterator parent, int pos) {
	super(_factory, obj, parent, pos);
    }

    /**
     * Returns the number of work functions for this Splitter.
     * @return number of work functions for this Splitter
     */
    public int getSplitterNumWork () {
	return SIRFineGrainedUtil.getSplitterNumWork(obj.getSplitter());
    }

    /**
     * Returns distribution of weights on a particular invocation
     * of work function for Splitter of this Stream.  The 
     * distribution is simply an array of ints, with numChildren 
     * elements.  The value at index 0 corresponds to number of items 
     * pushed out to 1st child, etc.
     * @return distribution of weights on a particular invocation
     * of work function for the Splitter of this Stream.
     */
    public int[] getSplitPushWeights (int nWork) {
	return SIRFineGrainedUtil.getSplitPushWeights(obj.getSplitter(), nWork);
    }

    /**
     * Returns number of data items consumed by a particular invocation
     * of work function for Splitter of this Stream.  These are the
     * items that will end up being pushed out to the children of this
     * Stream.
     * @return number of data items consumed by a particular invocation
     * of work function for Splitter of this Stream.
     */
    public int getSplitPop (int nWork) {
	return SIRFineGrainedUtil.getSplitPop(obj.getSplitter(), nWork);
    }

    /**
     * Returns the number of work functions for the Joiner
     * of this Stream.
     * @return number of work functions for the JOiner of this 
     * Stream
     */
    public int getJoinerNumWork () {
	return SIRFineGrainedUtil.getJoinerNumWork(obj.getJoiner());
    }

    /**
     * Returns distribution of weights on a particular invocation
     * of work function for Joiner of this SplitJoin.  The 
     * distribution is simply an array of ints, with numChildren 
     * elements.  The value at index 0 corresponds to number of items 
     * popped from 1st child, etc.
     * @return distribution of weights on a particular invocation
     * of work function for Joiner of this SplitJoin.
     */
    public int[] getJoinPopWeights (int nWork) {
	return SIRFineGrainedUtil.getJoinPopWeights(obj.getJoiner(), nWork);
    }
    
    /**
     * Returns number of data items produced by a particular invocation
     * of work function for Joiner of this SplitJoin.  These are the
     * items that were consumed from various children of this SplitJoin.
     * @return number of data items produced by a particular invocation
     * of work function for Joiner of this SplitJoin.
     */
    public int getJoinPush (int nWork) {
	return SIRFineGrainedUtil.getJoinPush(obj.getJoiner(), nWork);
    }
}
