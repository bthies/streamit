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
public final class HorizontalCutTransform extends StreamTransform {
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
	Utils.assert(str instanceof SIRSplitJoin, "Can only do horizontal cut on splitjoin, but got: " + str);
	// add one because of indexing convention in partitiongroup
	int[] partitions = { cutPos + 1 };
	RefactorSplitJoin.addHierarchicalChildren((SIRSplitJoin)str, 
						  PartitionGroup.createFromArray(partitions));
	return str;
    }
}
