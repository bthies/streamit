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

abstract class CConfigContainer extends CConfig {
    /**
     * Container we're wrapping
     */
    protected SIRContainer cont;
    /**
     * C[n][x1][x2] is best cost if children x1..x2 are assigned to
     * <n> tiles.
     */
    private CCost[][][] C; // cost for n tiles
    
    public CConfigContainer(SIRContainer cont, CachePartitioner partitioner) {
	super(partitioner);
	this.cont = cont;
	C = new CCost[0][cont.size()][cont.size()];
    }

    private void expandCostArray(int tileLimit) {
	if (C.length-1<tileLimit) {
	    // allocate new array
	    CCost[][][] old = C;
	    C = new CCost[tileLimit+1][cont.size()][cont.size()];
	    // copy over old contents
	    for (int i=0; i<old.length; i++) {
		C[i] = old[i];
	    }
	    // help gc
	    old = null;
	}
    }
    
    /**
     * Return the bottleneck work if this config is fit on <tileLimit>
     * tiles.
     */
    protected CCost get(int tileLimit) {
	// make sure our array is big enough
	expandCostArray(tileLimit);

	return get(0, cont.size()-1, tileLimit);
    }

    /**
     * Returns best cost for range of children x1..x2, inclusive, when
     * assigned to <tileLimit> tiles.
     */
    protected CCost get(int x1, int x2, int tileLimit) {
	//System.err.println("get(" + x1 + ", " + x2 + ", " + tileLimit + ")");

	// return it if it's memoized
	if (C[tileLimit][x1][x2]!=null) {
	    return C[tileLimit][x1][x2];
	}

	CCost result = null;
	// if down to one child, descend
	if (x1==x2) {
	    result = childConfig(x1).get(tileLimit);
	}

	// if down to one, then sum children
	else if (tileLimit==1) {
	    result = sumChildren(x1, x2);
	}

	// otherwise, try cuts (either vertical or horizontal,
	// depending on whether this is a splitjoin or pipeline)
	else {
	    CCost minCost = CCost.MAX_VALUE();
	    for (int xPivot=x1; xPivot<x2; xPivot++) {
		for (int tPivot=1; tPivot<tileLimit; tPivot++) {
		    CCost cost1 = get(x1, xPivot, tPivot);
		    CCost cost2 = get(xPivot+1, x2, tileLimit-tPivot);
		    CCost combo = CCost.combine(cost1, cost2);
		    if (minCost.greaterThan(combo)) {
			minCost = combo;
		    }
		}
	    }
	    result = minCost;
	}

	// memoize result
	assert result!=null;
	C[tileLimit][x1][x2] = result;
	return result;
    }

    /**
     * Traceback function.
     */
    public SIRStream traceback(LinkedList partitions, PartitionRecord curPartition, int tileLimit, SIRStream str) {
	SIRStream result = traceback(partitions, curPartition, 0, cont.size()-1, tileLimit, str);
	// if the whole container is assigned to one tile, record it
	// as such.
	if (tileLimit==1) {
	    IterFactory.createFactory().createIter(cont).accept(new RecordingStreamVisitor(curPartition));
	} 
	return result;
    }


    protected SIRStream traceback(LinkedList partitions, PartitionRecord curPartition,
				  int x1, int x2, int tileLimit, SIRStream str) {
	// if we're down to one node, then descend into it
	if (x1==x2) {
	    SIRStream child = childConfig(x1).traceback(partitions, curPartition, tileLimit, cont.get(x1));
	    return child;
	}

	
	if (this instanceof CConfigSplitJoin) {
	    int tiles = this.numberOfTiles();
	    if (tiles == 1) {
		return fuseAll(partitions, curPartition, x1, x2, tileLimit, str);   
	    }

	    if (x1 == x2) fuseAll(partitions, curPartition, x1, x2, tileLimit, str);   

	    return doCut(partitions, curPartition, x1, x2, x1, tileLimit, 0, str);
	}


	if (this instanceof CConfigPipeline) {
	    
	    int next = ((CConfigPipeline)this).findGreedyCut(x1, x2);
	    if (next == -1) {
		return fuseAll(partitions, curPartition, x1, x2, tileLimit, str);   
	    }
	    
	    return doCut(partitions, curPartition, x1, x2, next, tileLimit, 0, str);
	}
	

	/*

	// if we only have one tile left, return fusion transform with
	// children fused first
	if (tileLimit==1) {
	    return fuseAll(partitions, curPartition, x1, x2, tileLimit, str);
	}

	// otherwise, see if we made a vertical cut (breaking into
	// left/right pieces).
	for (int xPivot=x1; xPivot<x2; xPivot++) {
	    for (int tPivot=1; tPivot<tileLimit; tPivot++) {
		CCost cost1 = get(x1, xPivot, tPivot);
		CCost cost2 = get(xPivot+1, x2, tileLimit-tPivot);
		CCost combo = CCost.combine(cost1, cost2);
		if (combo.equals(C[tileLimit][x1][x2])) {
		    return doCut(partitions, curPartition, x1, x2, xPivot, tileLimit, tPivot, str);
		}
	    }
	}

	*/

	Utils.fail("Could not find traceback.");
	return null;
    }

    protected abstract SIRStream doCut(LinkedList partitions, PartitionRecord curPartition,
				       int x1, int x2, int xPivot, int tileLimit, int tPivot, SIRStream str);
    
    /**
     * Fuses everyone in range.
     */
    private SIRStream fuseAll(LinkedList partitions, PartitionRecord curPartition,
			      int x1, int x2, int tileLimit, SIRStream str) {
	// everything goes in this partition
	for (int x=x1; x<=x2; x++) {
	    IterFactory.createFactory().createIter(cont.get(x)).accept(new RecordingStreamVisitor(curPartition));
	}
	
	if (!CachePartitioner.transformOnTraceback) {
	    return str;
	} else {
	    // fuse everything.

	    //  This wrapper business is a mess.  Could probably be
	    //  simplified -- just moving legacy code out of end of
	    //  FuseAll, being sure to preserve functionality.
	    SIRPipeline wrapper = SIRContainer.makeWrapper(str);
	    wrapper.reclaimChildren();
	    SIRPipeline wrapper2 = FuseAll.fuse(str);
	    Lifter.eliminatePipe(wrapper2);
	    Lifter.lift(wrapper);
	    // make sure we've fused
	    assert wrapper.size()==1 &&
		wrapper.get(0) instanceof SIRFilter:
		"Wrapper contains " + wrapper.size() +
		" entries, with get(0)==" + wrapper.get(0);
	    // return child
	    Lifter.eliminatePipe(wrapper);
	    SIRStream result = wrapper.get(0);
	    return result;
	}
    }

    protected CConfig childConfig(int pos) {
	return partitioner.getConfig(cont.get(pos));
    }

    /**
     * Sum cost over children in positions x1...x2, inclusive.
     */
    private CCost sumChildren(int x1, int x2) {
	// if one of children is unfusable filter (e.g., FileWriter),
	// then return infinite cost
	if (unfusableFilter(x1, x2)) {
	    return new CCost(Integer.MAX_VALUE/2-1);
	}
	
	// check if this is a split join!

	if (this instanceof CConfigSplitJoin && (x1>0 || x2<cont.size()-1)) {
	    return new CCost(Integer.MAX_VALUE/2-1);
	}
	    
	if (this instanceof CConfigSplitJoin) {
	    return getFusionInfo().getCost();
	}

	// check if this is a pipeline

	if (this instanceof CConfigPipeline) {
	    CConfigPipeline pipe = (CConfigPipeline)this;
	    return pipe.getFusionInfo(x1, x2).getCost();
	}

	// not a split join, not a pipeline -> error

	assert (1 == 0);
	return new CCost(0);
    }

    /**
     * Returns whether or not this has an unfusable filter as a child
     * between range of positions <p1>, <p2>.  
     */
    private boolean unfusableFilter(int p1, int p2) {
	// if just one child, nothing to fuse with.
	if (p1==p2) {
	    return false;
	}
	// otherwise, look at all children
	for (int i=p1; i<=p2; i++) {
	    SIRStream str = cont.get(i);
	    if (str instanceof SIRFilter && !(isFusable((SIRFilter)str))) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Returns whether or not it is okay to fuse <filter> with others.
     */
    private boolean isFusable(SIRFilter filter) {
	// don't fuse message receivers because they have a lot of communication overhead
	SIRPortal[] portal = SIRPortal.getPortalsWithReceiver(filter);
	if (portal.length>=1) {
	    return false;
	}
	// don't fuse file readers or file writers
	if (filter instanceof SIRFileReader || filter instanceof SIRFileWriter) {
	    return false;
	}
	return true;
    }

    /**
     * Returns the fusion overhead of 2 filters.
     */
    protected abstract CCost fusionOverhead();

    public void printArray(int numTiles) {
	for (int i=0; i<cont.size(); i++) {
	    for (int j=0; j<cont.size(); j++) {
		for (int k=0; k<numTiles; k++) {
		    System.err.println("C[" + k + "][" + i + "][" + j + "] = " + C[k][i][j]);
		}
		System.err.println();
	    }
	}
    }

}
