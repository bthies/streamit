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

class DPConfigSplitJoin extends DPConfigContainer {
    public DPConfigSplitJoin(SIRSplitJoin split, DynamicProgPartitioner partitioner) {
	super(split, partitioner);
    }

    protected int get(int tileLimit) {
	/*
	  if (uniformSJ.contains(cont)) {
	  // optimize uniform splitjoins
	  return getUniform(tileLimit);
	  } else {
	*/
	// otherwise, use normal procedure
	if (tileLimit==1 || 
	    (cont.getParent()!=null && 
	     cont.getParent().getSuccessor(cont) instanceof SIRJoiner)) {
	    // if the tileLimit is 1 or joiner will be collapsed,
	    // then all tiles are available for the splitjoin
	    return super.get(tileLimit);
	} else {
	    // however, if we want to break the splitjoin up, then
	    // we have to subtract one from the tileLimit to
	    // account for the joiner tiles
	    return super.get(tileLimit-1);
	}
    }

    /**
     * Return bottleneck for uniform splitjoins.
     */
    private int getUniform(int tileLimit) {
	// get cost of child
	int childCost = childConfig(0).get(tileLimit);
	if (tileLimit<=2) {
	    // if one tile or two, have to fuse the whole thing
	    return childCost * cont.size();
	} else {
	    // otherwise, return the cost of a group.  subtract one
	    // tile to account for the joiner.
	    int groupSize = (int)Math.ceil(((double)cont.size()) / ((double)(tileLimit-1)));
	    return childCost * groupSize;
	}
    }

    /**
     * Do traceback for uniform splitjoins.
     */
    /*
      private int tracebackUniform(HashMap map, int[] tileCounter, int tileLimit) {
      }
    */

    public StreamTransform traceback(LinkedList partitions, PartitionRecord curPartition, int tileLimit) {
	/*
	  if (uniformSJ.contains(cont)) {
	  // optimize uniform splitjoins
	  tracebackUniform(map, tileCounter, tileLimit);
	  } else {
	*/
	StreamTransform result = null;
	if (tileLimit==1 || (cont.getParent()!=null && 
			     cont.getParent().getSuccessor(cont) instanceof SIRJoiner)) {
	    result = super.traceback(partitions, curPartition, tileLimit);
	} else {
	    // if we're not fusing into a single tile, need to:
	    // 1. decrease the tileLimit since one will be
	    // reserved for the joiner
	    result = super.traceback(partitions, curPartition, tileLimit-1);
	    // 2. add the joiner to the partition record
	    curPartition = new PartitionRecord();
	    curPartition.add(((SIRSplitJoin)cont).getJoiner(), 0);
	    partitions.add(curPartition);
	}

	return result;
    }
}
