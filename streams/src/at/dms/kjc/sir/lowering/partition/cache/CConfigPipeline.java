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

class CConfigPipeline extends CConfigContainer {
    
    public CConfigPipeline(SIRContainer cont, CachePartitioner partitioner) {
	super(cont, partitioner);
    }

    protected SIRStream doCut(LinkedList partitions, PartitionRecord curPartition,
			      int x1, int x2, int xPivot, int tileLimit, int tPivot, SIRStream str) {
	// there's a division at this <yPivot>.  We'll
	// return result of a horizontal cut.
	int[] arr = { 1 + (xPivot-x1), x2-xPivot };
	PartitionGroup pg = PartitionGroup.createFromArray(arr);
	
	SIRContainer result;
	// might have either pipeline or feedback loop at this point...
	if (str instanceof SIRPipeline) {
	    result = RefactorPipeline.addHierarchicalChildren((SIRPipeline)str, pg);
	} else if (str instanceof SIRFeedbackLoop) {
	    // if we have a feedbackloop, then factored is
	    // just the original, since it will have only
	    // two children
	    result = (SIRContainer)str;
	} else {
	    result = null;
	    Utils.fail("Unrecognized stream type: " + str);
	}
	
	// recurse up and down
	SIRStream top = traceback(partitions, curPartition, x1, xPivot, tPivot, result.get(0));
	// mark that we have a partition here
	curPartition = new PartitionRecord();
	partitions.add(curPartition);
	SIRStream bot = traceback(partitions, curPartition, xPivot+1, x2, tileLimit-tPivot, result.get(1));
	
	// mutate ourselves if we haven't been mutated yet
	result.set(0, top);
	result.set(1, bot);
	
	// all done
	return result;
    }

    /**
     * For now, fusion overhead of a pipeline is 0.
     */
    private static final CCost fusionOverhead = new CCost(0, 0);
    protected CCost fusionOverhead() {
	return fusionOverhead;
    }
    
}
