package at.dms.kjc.iterator;

/**
 * This class is the outside interface for managing iterators.
 */

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;

public class IterFactory {

    public static SIRPipelineIter createIter(SIRPipeline obj) { 
	return new SIRPipelineIter(obj);
    }

    public static SIRFilterIter createIter(SIRFilter obj) { 
	return new SIRFilterIter(obj);
    }

    public static SIRSplitJoinIter createIter(SIRSplitJoin obj) {
	return new SIRSplitJoinIter(obj);
    }

    public static SIRFeedbackLoopIter createIter(SIRFeedbackLoop obj) {
	return new SIRFeedbackLoopIter(obj);
    }

    public static SIRIterator createIter(SIRStream obj) { 
	if (obj instanceof SIRFilter) {
	    return createIter((SIRFilter)obj);
	} else if (obj instanceof SIRPipeline) {
	    return createIter((SIRPipeline)obj);
	} else if (obj instanceof SIRSplitJoin) {
	    return createIter((SIRSplitJoin)obj);
	} else if (obj instanceof SIRFeedbackLoop) {
	    return createIter((SIRFeedbackLoop)obj);
	} else {
	    Utils.fail("Unexpected iterator " + obj + " of type " 
		       + (obj==null ? "" : obj.getClass().toString()));
	    return null;
	}
    }

    /**
     * - does cloning
     * - replaces in parents by shallowclone and replacement
     */
    SIRIterator getMutableCopy(SIRIterator i) { return null; }

    /**
     * - does finalization of contained stream structures
     * - does structural equality test
     * - replaces in parents by shallowclone and replacement
     */
    SIRIterator finalize(SIRIterator i) { return null; }

    /**
     * For building a child iterator, internal only to the iterator
     * package.
     */
    static SIRIterator createIter(SIRStream obj, SIRIterator parent, int pos) { 
	if (obj instanceof SIRFilter) {
	    return new SIRFilterIter((SIRFilter)obj, parent, pos);
	} else if (obj instanceof SIRPipeline) {
	    return new SIRPipelineIter((SIRPipeline)obj, parent, pos);
	} else if (obj instanceof SIRSplitJoin) {
	    return new SIRSplitJoinIter((SIRSplitJoin)obj, parent, pos);
	} else if (obj instanceof SIRFeedbackLoop) {
	    return new SIRFeedbackLoopIter((SIRFeedbackLoop)obj, parent, pos);
	} else {
	    Utils.fail("Unexpected iterator " + obj + " of type " 
		       + (obj==null ? "" : obj.getClass().toString()));
	    return null;
	}
    }

}
