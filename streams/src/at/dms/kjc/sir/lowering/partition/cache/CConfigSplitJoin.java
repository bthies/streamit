package at.dms.kjc.sir.lowering.partition.cache;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.partition.*;

class CConfigSplitJoin extends CConfigContainer {
    
    public CConfigSplitJoin(SIRSplitJoin sj, CachePartitioner partitioner) {
	super(sj, partitioner);
    }
    
    protected SIRStream doCut(LinkedList partitions, PartitionRecord curPartition,
			      int x1, int x2, int xPivot, int tileLimit, int tPivot, SIRStream str) {
	// there's a division at this <xPivot>.  We'll
	// return result of a vertical cut
	int[] arr = { 1 + (xPivot-x1), x2-xPivot };
	PartitionGroup pg = PartitionGroup.createFromArray(arr);
	// do the vertical cut
	SIRSplitJoin sj = RefactorSplitJoin.addHierarchicalChildren((SIRSplitJoin)str, pg);
	
	// recurse left and right.
	SIRStream left = traceback(partitions, curPartition, x1, xPivot, tPivot, sj.get(0));
	// mark that we have a partition here
	curPartition = new PartitionRecord();
	partitions.add(curPartition);
	SIRStream right = traceback(partitions, curPartition, xPivot+1, x2, tileLimit-tPivot, sj.get(1));
	
	// mutate ourselves if we haven't been mutated already
	sj.set(0, left);
	sj.set(1, right);
	
	return sj;
    }

    /**
     * Mark that horizontal fusion in splitjoin costs something.
     */
    private static final CCost fusionOverhead = new CCost(1, 1);
    protected CCost fusionOverhead() {
	return fusionOverhead;
    }
}
