package at.dms.kjc.sir.lowering.partition.dynamicprog;

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

class DPConfigFilter extends DPConfig {
    /**
     * The filter corresponding to this.
     */
    private SIRFilter filter;
    /**
     * Whether or not <filter> is stateless.
     */
    private boolean isFissable;
	
    public DPConfigFilter(SIRFilter filter, DynamicProgPartitioner partitioner) {
	super(partitioner);
	this.filter = filter;
	this.isFissable = StatelessDuplicate.isFissable(filter);
	this.A = null;
    }

    public int get(int tileLimit) {
	int workCount = partitioner.getWorkEstimate().getWork(filter);
	// return decreased work if we're fissable
	if (tilesForFission(tileLimit)>1 && isFissable) {
	    /*
	      System.err.println("Trying " + filter + " on " + tileLimit + " tiles and splitting " + 
	      tilesForFission(tileLimit) + " ways with bottlneck of " + 
	      workCount / tilesForFission(tileLimit) + FISSION_OVERHEAD);
	    */
	    return workCount / tilesForFission(tileLimit) + DynamicProgPartitioner.FISSION_OVERHEAD;
	} else {
	    return workCount;
	}
    }

    // see how many tiles we can devote to fissed filters;
    // depends on if we need a separate tile for a joiner.
    // This is a conservative approximation (joiner disappears
    // if next downstream filter is a joiner, even if parent
    // is not sj)
    private int tilesForFission(int tileLimit) {
	return (filter.getParent()!=null && 
		filter.getParent().getSuccessor(filter) instanceof SIRJoiner ?
		tileLimit : 
		tileLimit - 1);
    }

    public SIRStream getStream() {
	return filter;
    }

    /**
     * Requires <str> is a filter.
     */
    protected void setStream(SIRStream str) {
	Utils.assert(str instanceof SIRFilter);
	this.filter = (SIRFilter)str;
    }

    /**
     * Add this to the map and return.
     */
    public StreamTransform traceback(LinkedList partitions, PartitionRecord curPartition, int tileLimit) {
	// do fission if we can
	int tff = tilesForFission(tileLimit);
	if (tff>1 && isFissable) {
	    // record fission tiles
	    for (int i=0; i<tff; i++) {
		curPartition.add(filter, 
				 partitioner.getWorkEstimate().getWork(filter) / tff + 
				 DynamicProgPartitioner.FISSION_OVERHEAD);
		// only draw new partitions BETWEEN the elements
		// of the fissed splitjoin -- not at the end
		if (i!=tff-1) {
		    partitions.add(curPartition);
		    curPartition = new PartitionRecord();
		}
	    }
	    // record joiner tile if we need one
	    if (tff<tileLimit) {
		curPartition = new PartitionRecord();
		// just record the filter as the contents since
		// it's responsible for this joiner node
		curPartition.add(SIRJoiner.createUniformRR(filter.getParent(), new JIntLiteral(1)), 0);
		partitions.add(curPartition);
	    }
	    return new FissionTransform(tilesForFission(tileLimit));
	} else {
	    curPartition.add(filter, partitioner.getWorkEstimate().getWork(filter));
	    return new IdentityTransform();
	}
    }
}
