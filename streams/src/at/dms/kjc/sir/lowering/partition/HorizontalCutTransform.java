package at.dms.kjc.sir.lowering.partition;

import java.util.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;

/**
 * Horizontal cut transform on a stream graph.
 */
public final class HorizontalCutTransform extends IdempotentTransform {
    /**
     * Position of the cut.  That is, the index of the last child in
     * the splitjoin that is included in the LEFT piece (after the cut
     * is made).
     */
    private int cutPos;

    public HorizontalCutTransform(int cutPos) {
	super();
	this.cutPos = cutPos;
    }

    /**
     * Perform the transform on <str> and return new stream.  Requires
     * that <str> is a SplitJoin.
     */
    public SIRStream doMyTransform(SIRStream str) {
	if (str instanceof SIRPipeline) {
	    // represents a cut in the immediate children of a pipeline
	    SIRPipeline pipe = (SIRPipeline)str;
	    assert pipe.size() - cutPos - 1 > 0:
                "Don't allow cuts with zero items on one side";
	    // add one because of indexing convention in partitiongroup
	    int[] partitions = { cutPos + 1 , pipe.size() - cutPos - 1 };
	    PartitionGroup group = PartitionGroup.createFromArray(partitions);
	    return RefactorPipeline.addHierarchicalChildren(pipe, group);
	} else if (str instanceof SIRSplitJoin) {
	    // represents a cut in the children's children, since we are cutting each pipeline
	    SIRSplitJoin sj = (SIRSplitJoin)str;
	    assert sj.getRectangularHeight() - cutPos - 1 > 0:
                "Don't allow cuts with zero items on one side";
	    // add one because of indexing convention in partitiongroup
	    int[] partitions = { cutPos + 1 , sj.getRectangularHeight() - cutPos - 1 };
	    PartitionGroup group = PartitionGroup.createFromArray(partitions);
	    return RefactorSplitJoin.addSyncPoints(sj, group);
	} else if (str instanceof SIRFeedbackLoop) {
	    assert cutPos==0:
                "Trying to horizontal cut a feedbackloop at position " +
                cutPos;
	    // a feedbackloop already has a single cut, so just return it
	    return str;
	} else {
	    Utils.fail("Expected Pipeline or SplitJoin, but got: " + str.getClass());
	    return str;
	}
    }

    public String toString() {
	return "Horizontal Cut transform, #" + id + " (pos = " + cutPos + ")";
    }
}
