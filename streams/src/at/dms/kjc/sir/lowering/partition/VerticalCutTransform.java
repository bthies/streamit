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
public final class VerticalCutTransform extends StreamTransform {
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
	Utils.assert(str instanceof SIRSplitJoin, "Only support vertical cuts on splitjoins, but got "  + str.getClass());
	SIRSplitJoin sj = (SIRSplitJoin)str;
	Utils.assert(sj.size() - cutPos - 1 > 0, "Don't allow cuts with zero items on one side");

	// add one because of indexing convention in partitiongroup
	int[] partitions = { cutPos + 1, sj.size() - cutPos - 1};
	PartitionGroup group = PartitionGroup.createFromArray(partitions);
	RefactorSplitJoin.addHierarchicalChildren((SIRSplitJoin)sj, group);
	return sj;
    }

    public String toString() {
	return "Vertical Cut transform (pos = " + cutPos + ")";
    }
}
