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

abstract class DPConfigContainer extends DPConfig {
    /**
     * The stream for this container.
     */
    protected SIRContainer cont;
    /**
     * Work estimate containing our children.
     */
    protected WorkEstimate work;
    
    /**
     * <width> and <height> represent the dimensions of the stream.
     */
    protected DPConfigContainer(SIRContainer cont, DynamicProgPartitioner partitioner, 
				int width, int height) {
	super(partitioner);
	this.cont = cont;
	this.work = partitioner.getWorkEstimate();
	this.A = new int[width][width][height][height][partitioner.getNumTiles()+1][2];
    }

    public SIRStream getStream() {
	return cont;
    }

    /**
     * Requires <str> is a container.
     */
    protected void setStream(SIRStream str) {
	Utils.assert(str instanceof SIRContainer);
	this.cont = (SIRContainer)str;
    }

    protected int get(int tileLimit, int nextToJoiner) {
	// otherwise, compute it
	return get(0, A.length-1, 0, A[0][0].length-1, tileLimit, nextToJoiner);
    }

    protected int get(int x1, int x2, int y1, int y2, int tileLimit, int nextToJoiner) {
	// if we've memoized the value before, return it
	if (A[x1][x2][y1][y2][tileLimit][nextToJoiner]>0) {
	    /*
	      System.err.println("Found memoized A[" + child1 + "][" + child2 + "][" + tileLimit + "] = " + 
	      A[child1][child2][tileLimit] + " for " + cont.getName());
	    */
	    return A[x1][x2][y1][y2][tileLimit][nextToJoiner];
	}

	// if we are down to one child, then descend into child
	if (x1==x2 && y1==y2) {
	    int childCost = childConfig(x1, y1).get(tileLimit, nextToJoiner); 
	    A[x1][x2][y1][y2][tileLimit][nextToJoiner] = childCost;
	    //System.err.println("Returning " + childCost + " from descent into child.");
	    return childCost;
	}

	// otherwise, if <tileLimit> is 1, then just sum the work
	// of our components
	if (tileLimit==1) {
	    int sum = get(x1, x1, y1, y1, tileLimit, nextToJoiner);
	    sum += x1<x2 ? get( x1+1, x2, y1, y1, tileLimit, nextToJoiner) : 0;
	    sum += y1<y2 ? get(x1, x1, y1+1, y2, tileLimit, nextToJoiner) : 0;
	    sum += x1<x2 && y1<y2 ? get(x1+1, x2, y1+1, y2, tileLimit, nextToJoiner) : 0;
	    // since we went to down to one child, the cost is the
	    // same whether or not there is a joiner, so record both
	    // ways.
	    A[x1][x2][y1][y2][tileLimit][0] = sum;
	    A[x1][x2][y1][y2][tileLimit][1] = sum;
	    //System.err.println("Returning sum " + sum + " from fusion.");
	    return sum;
	}

	// otherwise, try making a vertical cut.  A vertical cut will
	// necessitate a joiner at this node, if we don't already have
	// one.  This has two consequences: 1) the nextToJoiner
	// argument for children will be true (1), and 2) A tile is
	// used by the joiner.  We represent #2 by tilesAvail, the
	// number of tailes available after the joiner is taken.
	int tilesAvail = tileLimit - (1-nextToJoiner);
	int min = Integer.MAX_VALUE;
	for (int xPivot=x1; xPivot<x2; xPivot++) {
	    for (int tPivot=1; tPivot<tilesAvail; tPivot++) {
		int cost = Math.max(getWithFusionOverhead(x1, xPivot, y1, y2, tPivot, 1, tilesAvail),
				    getWithFusionOverhead(xPivot+1, x2, y1, y2, tilesAvail-tPivot, 1, tilesAvail));
		if (cost < min) {
		    //System.err.println("possible vertical cut at x=" + xPivot + " from y=" + y1 + " to y=" + y2 + " in " + cont.getName());
		    min = cost;
		}
	    }
	}

	// try making horizontal cut (for splitjoin, pipeline,
	// feedbackloop).  In this case, we keep whatever joiner we
	// have for the bottom piece of the cut, but the top piece
	// will need to make its own joiner.  The arguments are thus
	// false (0) for the top, and true (1) for the bottom.
	for (int yPivot=y1; yPivot<y2; yPivot++) {
	    for (int tPivot=1; tPivot<tileLimit; tPivot++) {
		int cost = Math.max(getWithFusionOverhead(x1, x2, y1, yPivot, tPivot, 0, tileLimit),
				    getWithFusionOverhead(x1, x2, yPivot+1, y2, tileLimit-tPivot, nextToJoiner, tileLimit));
		if (cost < min) {
		    //System.err.println("possible horizontal cut at y=" + yPivot + " from x=" + x1 + " to x=" + x2 + " in " + cont.getName());
		    min = cost;
		}
	    }
	}
	
	A[x1][x2][y1][y2][tileLimit][nextToJoiner] = min;
	return min;
    }

    /**
     * <tileLimit> is number of tiles for this partition of children.
     * <tilesAvail> is number of tiles that were available in parent.
     */
    private int getWithFusionOverhead(int x1, int x2, int y1, int y2, int tileLimit, int nextToJoiner, int tilesAvail) {
	// get cost
	int cost = get(x1, x2, y1, y2, tileLimit, nextToJoiner);
	int overhead = 0;
	// add rough estimate of overhead for horizontal fusion.  Do
	// this at the toplevel node--where we had more tiles before,
	// but only one afterwards.
	if (tileLimit==1 && tilesAvail>1 && x1<x2) {
	    // for filters, add cost estimate according to their
	    // rates; otherwise, add generic cost estimate...
	    // do input filters
	    for (int i=x1; i<=x2; i++) {
		DPConfig config = childConfig(i,y1);
		if (config instanceof DPConfigFilter) {
		    // add input rate
		    SIRFilter filter = (SIRFilter)config.getStream();
		    overhead += filter.getPopInt() * work.getReps(filter) * DynamicProgPartitioner.HORIZONTAL_FILTER_OVERHEAD_FACTOR;
		} else {
		    // add generic rate
		    overhead += DynamicProgPartitioner.HORIZONTAL_CONTAINER_OVERHEAD;
		}
	    }
	    // do output filters
	    for (int i=x1; i<=x2; i++) {
		DPConfig config = childConfig(i,y2);
		if (config instanceof DPConfigFilter) {
		    // add input rate
		    SIRFilter filter = (SIRFilter)config.getStream();
		    overhead += filter.getPushInt() * DynamicProgPartitioner.HORIZONTAL_FILTER_OVERHEAD_FACTOR;
		} else {
		    // add generic rate
		    overhead += DynamicProgPartitioner.HORIZONTAL_CONTAINER_OVERHEAD;
		}
	    }
	}
	return cost + overhead;
    }

    /**
     * Traceback function.
     */
    public StreamTransform traceback(LinkedList partitions, PartitionRecord curPartition, int tileLimit, int nextToJoiner) {
	StreamTransform st = traceback(partitions, curPartition, 0, A.length-1, 0, A[0][0].length-1, tileLimit, nextToJoiner);
	// if the whole container is assigned to one tile, record it
	// as such.
	if (tileLimit==1) {
	    curPartition.add(cont);
	} 
	return st;
    }
	
    /**
     * Traceback helper function.
     */
    protected StreamTransform traceback(LinkedList partitions, PartitionRecord curPartition,
					int x1, int x2, int y1, int y2, int tileLimit, int nextToJoiner) {

	// if we're down to one node, then descend into it
	if (x1==x2 && y1==y2) {
	    StreamTransform child = childConfig(x1, y1).traceback(partitions, curPartition, tileLimit, nextToJoiner);
	    // if this config container only has one child, then we
	    // should wrap this in an identity so that we don't apply
	    // it to ourself
	    if (A.length==1 && A[0][0].length==1) {
		StreamTransform result = new IdentityTransform();
		result.addSucc(child);
		return result.reduce();
	    } else {
		return child.reduce();
	    }
	}
	
	// if we only have one tile left, return fusion transform with
	// children fused first
	if (tileLimit==1) {
	    FusionTransform result = new FusionTransform();
	    result.addPartition(0);
	    if (x1<x2) {
		// if there are horizontal streams, fuse them first
		result.addPartition(1+x2-x1);
		for (int x=x1; x<=x2; x++) {
		    result.addPred(traceback(partitions, curPartition, x, x, y1, y2, tileLimit, nextToJoiner));
		}
	    } else {
		// otherwise, fuse the vertical streams
		result.addPartition(1+y2-y1);
		for (int y=y1; y<=y2; y++) {
		    result.addPred(traceback(partitions, curPartition, x1, x2, y, y, tileLimit, nextToJoiner));
		}
	    }
	    return result.reduce();
	}

	// otherwise, see if we made a vertical cut (breaking into
	// left/right pieces).  As with get procedure, pass
	// nextToJoiner as true and adjust tileLimit around the call.
	int tilesAvail = tileLimit - (1-nextToJoiner);
	for (int xPivot=x1; xPivot<x2; xPivot++) {
	    for (int tPivot=1; tPivot<tilesAvail; tPivot++) {
		int cost = Math.max(getWithFusionOverhead(x1, xPivot, y1, y2, tPivot, 1, tilesAvail),
				    getWithFusionOverhead(xPivot+1, x2, y1, y2, tilesAvail-tPivot, 1, tilesAvail));
		if (cost==A[x1][x2][y1][y2][tileLimit][nextToJoiner]) {
		    // there's a division at this <xPivot>.  We'll
		    // return a vertical cut
		    //System.err.println("tracing vertical cut at x=" + xPivot + " from y=" + y1 + " to y=" + y2 + " in " + cont.getName());
		    StreamTransform result = new VerticalCutTransform(xPivot-x1);
		    // recurse left and right, adding transforms as post-ops
		    result.addSucc(traceback(partitions, curPartition, x1, xPivot, y1, y2, tPivot, 1));
		    // mark that we have a partition here
		    curPartition = new PartitionRecord();
		    partitions.add(curPartition);
		    result.addSucc(traceback(partitions, curPartition, xPivot+1, x2, y1, y2, tilesAvail-tPivot, 1));
		    if (nextToJoiner==0) {
			// if we have to add a joiner, then add it to the partition record
			curPartition = new PartitionRecord();
			curPartition.add(((SIRSplitJoin)cont).getJoiner(), 0);
			partitions.add(curPartition);
		    }
		    // all done
		    return result.reduce();
		}
	    }
	}

	// otherwise, see if we made a horizontal cut (breaking into
	// top/bottom pieces).  Pass nextToJoiner to bottom child
	// since it will share our joiner if we have one, but 0 to top
	// child since it will need its own.
	for (int yPivot=y1; yPivot<y2; yPivot++) {
	    for (int tPivot=1; tPivot<tileLimit; tPivot++) {
		int cost = Math.max(getWithFusionOverhead(x1, x2, y1, yPivot, tPivot, 0, tileLimit),
				    getWithFusionOverhead(x1, x2, yPivot+1, y2, tileLimit-tPivot, nextToJoiner, tileLimit));
		if (cost==A[x1][x2][y1][y2][tileLimit][nextToJoiner]) {
		    // there's a division at this <yPivot>.  We'll
		    // return a horizontal cut.
		    //System.err.println("tracing horizontal cut at y=" + yPivot + " from x=" + x1 + " to x=" + x2 + " in " + cont.getName());
		    StreamTransform result = new HorizontalCutTransform(yPivot-y1);
		    // recurse left and right, adding transforms as post-ops
		    result.addSucc(traceback(partitions, curPartition, x1, x2, y1, yPivot, tPivot, 0));
		    // mark that we have a partition here
		    curPartition = new PartitionRecord();
		    partitions.add(curPartition);
		    result.addSucc(traceback(partitions, curPartition, x1, x2, yPivot+1, y2, tileLimit-tPivot, nextToJoiner));
		    // all done
		    return result.reduce();
		}
	    }
	}
	
	// if we make it this far, then we didn't find our traceback
	Utils.fail("Didn't find traceback.");
	return null;
    }

    /**
     * Returns config for child at index <x, y>
     */
    protected abstract DPConfig childConfig(int x, int y);
}
