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

    public DPConfigSplitJoin(SIRSplitJoin sj, DynamicProgPartitioner partitioner) {
	super(sj, partitioner, sj.size(), getHeight(sj));
    }

    protected DPConfig childConfig(int x, int y) {
	SIRStream c1 = cont.get(x), c2;
	// if we're just accessing a hierarchical unit, return it
	if (y==0 && !(c1 instanceof SIRPipeline)) {
	    c2 = c1;
	} else {
	    // otherwise, we're looking inside a hierarchical unit -- must
	    // be a pipeline
	    Utils.assert(c1 instanceof SIRPipeline);
	    c2 = ((SIRPipeline)c1).get(y);
	}
	return partitioner.getConfig(c2);
    }

    /**
     * Returns the maximum length of a parallel stream in <sj>.
     */
    private static int getHeight(SIRSplitJoin sj) {
	int height = getComponentHeight(sj.get(0));
	// for now, only deal with splitjoins that are rectangular.
	// should extend with identities for the general case.
	for (int i=1; i<sj.size(); i++) {
	    Utils.assert(height==getComponentHeight(sj.get(i)),
			 "Detected non-rectangular splitjoin: " + sj + "\n" +
			 "Only deal with rectangular for now.");
	}
	return height;
    }

    /**
     * Returns the height of <str>.  Everything but pipelines have a
     * height of 1 since they are treated as a hierarchical unit.
     */
    private static int getComponentHeight(SIRStream str) {
	if (str instanceof SIRPipeline) {
	    return ((SIRPipeline)str).size();
	} else {
	    return 1;
	}
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
	int childCost = childConfig(0, 0).get(tileLimit);
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
