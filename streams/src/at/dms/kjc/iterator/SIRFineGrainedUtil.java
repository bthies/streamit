package at.dms.kjc.iterator; 

import at.dms.kjc.sir.*;
import at.dms.util.*;
import streamit.scheduler2.iriter.*;

/**
 * Represents the fine-grained calculations for splitters and joiners,
 * whether they are in feedbackloops or splitjoins.
 */

class SIRFineGrainedUtil {

    /**
     * Returns the number of work functions for this Splitter.
     * @return number of work functions for this Splitter
     */
    static int getSplitterNumWork (SIRSplitter splitter) {
	if (splitter.getType()==SIRSplitType.DUPLICATE) {
	    return 1;
	} else if (splitter.getType()==SIRSplitType.NULL) {
	    return 1;
	} else {
	    int sum = splitter.getSumOfWeights();
	    // if the sum is zero, still represent that we have 1 work function
	    if (sum==0) {
		return 1;
	    } else {
		return sum;
	    }
	}
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
    static int[] getSplitPushWeights (SIRSplitter splitter, int nWork) {
	if ((splitter.getType()==SIRSplitType.DUPLICATE ||
	     splitter.getType()==SIRSplitType.NULL)) {
	    return splitter.getWeights();
	} else {
	    return getWeightsForPhase(nWork, splitter.getWeights());
	}
    }

    /**
     * Given that <weights> are some weights for a splitter or joiner,
     * return the amount that is consumed/produced on each channel
     * during phase number <phase>, assuming fine-grained phases where
     * only 1 item passes through in a given phase.
     */
    private static int[] getWeightsForPhase(int phase, int[] weights) {
	// for round-robins, we simulate a single item going
	// through at a time.  Figure out which stream the
	// nWork'th item will go to.
	int targetStream=-1;
	int sum=0;
	do {
	    targetStream++;
	    sum+=weights[targetStream];
	} while (sum<(phase+1) && targetStream<weights.length-1);
	// if we hit the end and the sum is still zero, then there
	// are no push weights, and we return an empty array
	if (sum==0) {
	    // weights is all zero (and not a rep exposure)
	    return weights;
	} else {
	    // otherwise, we return an array that is zero
	    // everywhere except targetStream, where it should be
	    // "1"
	    int[] result = new int[weights.length];
	    result[targetStream] = 1;
	    return result;
	}
    }
    
    /**
     * Returns number of data items consumed by a particular invocation
     * of work function for Splitter of this Stream.  These are the
     * items that will end up being pushed out to the children of this
     * Stream.
     * @return number of data items consumed by a particular invocation
     * of work function for Splitter of this Stream.
     */
    static int getSplitPop (SIRSplitter splitter, int nWork) {
	if (splitter.getType()==SIRSplitType.DUPLICATE) {
	    return 1;
	} else if (splitter.getType()==SIRSplitType.NULL) {
	    return 0;
	} else {
	    // if we consume anything, we will consume 1
	    int sum = splitter.getSumOfWeights();
	    if (sum==0) {
		return 0;
	    } else {
		return 1;
	    }
	}
    }

    /**
     * Returns the number of work functions for the Joiner
     * of this Stream.
     * @return number of work functions for the JOiner of this 
     * Stream
     */
    static int getJoinerNumWork (SIRJoiner joiner) {
	// have as many work functions as there are weights, making
	// sure to have at least 1 work function
	int sum = joiner.getSumOfWeights();
	if (sum==0) {
	    return 1;
	} else {
	    return sum;
	}
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
    static int[] getJoinPopWeights (SIRJoiner joiner, int nWork) {
	if (joiner.getType()==SIRJoinType.NULL) {
	    return joiner.getWeights();
	} else {
	    return getWeightsForPhase(nWork, joiner.getWeights());
	}
    }
    
    /**
     * Returns number of data items produced by a particular invocation
     * of work function for Joiner of this SplitJoin.  These are the
     * items that were consumed from various children of this SplitJoin.
     * @return number of data items produced by a particular invocation
     * of work function for Joiner of this SplitJoin.
     */
    static int getJoinPush (SIRJoiner joiner, int nWork) {
	// if we push anything, we push 1 per phase
	int sum = joiner.getSumOfWeights();
	if (sum==0) {
	    return 0;
	} else {
	    return 1;
	}
    }
}
