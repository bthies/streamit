package at.dms.kjc.sir.lowering.partition;

import java.util.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;

/**
 * Vertical cut transform on a stream graph.
 */
public final class VerticalCutTransform extends IdempotentTransform {
    /**
     * Position of the cut.  That is, the index of the last child that
     * is included in the TOP piece (after the cut is made).
     */
    private int cutPos;

    public VerticalCutTransform(int cutPos) {
	super();
	this.cutPos = cutPos;
    }

    /**
     * Perform the transform on <str> and return new stream.
     */
    public SIRStream doMyTransform(SIRStream str) {
	if (str instanceof SIRSplitJoin) {
	    SIRSplitJoin sj = (SIRSplitJoin)str;
	    assert sj.size() - cutPos - 1 > 0:
                "Don't allow cuts with zero items on one side";
	    
	    // add one because of indexing convention in partitiongroup
	    int[] partitions = { cutPos + 1, sj.size() - cutPos - 1};
	    PartitionGroup group = PartitionGroup.createFromArray(partitions);
	    SIRSplitJoin factored = RefactorSplitJoin.addHierarchicalChildren((SIRSplitJoin)sj, group);
	    // now we have to add back any synchronization, since
	    // that's the canonical form of the partitioner
	    for (int i=0; i<factored.size(); i++) {
		if (factored.get(i) instanceof SIRSplitJoin) {
		    SIRSplitJoin toSync = (SIRSplitJoin)factored.get(i);
		    if (toSync.getRectangularHeight()>1) {
			factored.set(i, RefactorSplitJoin.addSyncPoints(toSync, PartitionGroup.createUniformPartition(toSync.getRectangularHeight())));
		    }
		}
	    }
	    return factored;
	} else if (str instanceof SIRFeedbackLoop) {
	    assert cutPos==0:
                "Trying to vertical cut feedbackloop at position " + cutPos;
	    // a cut at pos 1 is equivalent to breaking this guy in half
	    return str;
	} else {
	    Utils.fail("Don't support vertical cuts on type "  + str.getClass() + " but trying it at position " + cutPos + " of " + str);
	    return null;
	}
    }

    public String toString() {
	return "Vertical Cut transform, #" + id + " (pos = " + cutPos + ")";
    }
}
