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
     * <width> and <height> represent the dimensions of the stream.
     */
    protected DPConfigContainer(SIRContainer cont, DynamicProgPartitioner partitioner, 
				int width, int height) {
	super(partitioner);
	this.cont = cont;
	this.A = new int[width][width][height][height][partitioner.getNumTiles()+1];
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

    protected int get(int tileLimit) {
	// otherwise, compute it
	return get(0, A.length-1, 0, A[0][0].length-1, tileLimit);
    }

    protected int get(int x1, int x2, int y1, int y2, int tileLimit) {
	// if we've memoized the value before, return it
	if (A[x1][x2][y1][y2][tileLimit]>0) {
	    /*
	      System.err.println("Found memoized A[" + child1 + "][" + child2 + "][" + tileLimit + "] = " + 
	      A[child1][child2][tileLimit] + " for " + cont.getName());
	    */
	    return A[x1][x2][y1][y2][tileLimit];
	}

	// if we are down to one child, then descend into child
	if (x1==x2 && y1==y2) {
	    int childCost = childConfig(x1, y1).get(tileLimit); 
	    A[x1][x2][y1][y2][tileLimit] = childCost;
	    //System.err.println("Returning " + childCost + " from descent into child.");
	    return childCost;
	}	    

	// otherwise, if <tileLimit> is 1, then just sum the work
	// of our components
	if (tileLimit==1) {
	    int sum = get(x1, x1, y1, y1, tileLimit);
	    sum += x1<x2 ? get( x1+1, x2, y1, y1, tileLimit) : 0;
	    sum += y1<y2 ? get(x1, x1, y1+1, y2, tileLimit) : 0;
	    sum += x1<x2 && y1<y2 ? get(x1+1, x2, y1+1, y2, tileLimit) : 0;
	    A[x1][x2][y1][y2][tileLimit] = sum;
	    //System.err.println("Returning sum " + sum + " from fusion.");
	    return sum;
	}

	// otherwise, try making a vertical cut
	int min = Integer.MAX_VALUE;
	for (int xPivot=x1; xPivot<x2; xPivot++) {
	    for (int tPivot=1; tPivot<tileLimit; tPivot++) {
		int cost = Math.max(get(x1, xPivot, y1, y2, tPivot),
				    get(xPivot+1, x2, y1, y2, tileLimit-tPivot));
		if (cost < min) {
		    //System.err.println("possible vertical cut at x=" + xPivot + " from y=" + y1 + " to y=" + y2 + " in " + cont.getName());
		    min = cost;
		}
	    }
	}

	// try making horizontal cut (for splitjoin, pipeline, feedbackloop)
	for (int yPivot=y1; yPivot<y2; yPivot++) {
	    for (int tPivot=1; tPivot<tileLimit; tPivot++) {
		int cost = Math.max(get(x1, x2, y1, yPivot, tPivot),
				    get(x1, x2, yPivot+1, y2, tileLimit-tPivot));
		if (cost < min) {
		    //System.err.println("possible horizontal cut at y=" + yPivot + " from x=" + x1 + " to x=" + x2 + " in " + cont.getName());
		    min = cost;
		}
	    }
	}
	
	A[x1][x2][y1][y2][tileLimit] = min;
	return min;
    }

    /**
     * Traceback function.
     */
    public StreamTransform traceback(LinkedList partitions, PartitionRecord curPartition, int tileLimit) {
	StreamTransform st = traceback(partitions, curPartition, 0, A.length-1, 0, A[0][0].length-1, tileLimit);
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
					int x1, int x2, int y1, int y2, int tileLimit) {

	// if we're down to one node, then descend into it
	if (x1==x2 && y1==y2) {
	    return childConfig(x1, y1).traceback(partitions, curPartition, tileLimit);
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
		    result.addPred(traceback(partitions, curPartition, x, x, y1, y2, tileLimit));
		}
	    } else {
		// otherwise, fuse the vertical streams
		result.addPartition(1+y2-y1);
		for (int y=y1; y<=y2; y++) {
		    result.addPred(traceback(partitions, curPartition, x1, x2, y, y, tileLimit));
		}
	    }
	    return result;
	}

	// otherwise, see if we made a vertical cut (breaking into left/right pieces)
	for (int xPivot=x1; xPivot<x2; xPivot++) {
	    for (int tPivot=1; tPivot<tileLimit; tPivot++) {
		int cost = Math.max(get(x1, xPivot, y1, y2, tPivot),
				    get(xPivot+1, x2, y1, y2, tileLimit-tPivot));
		if (cost==A[x1][x2][y1][y2][tileLimit]) {
		    // there's a division at this <xPivot>.  We'll
		    // return a vertical cut
		    //System.err.println("tracing vertical cut at x=" + xPivot + " from y=" + y1 + " to y=" + y2 + " in " + cont.getName());
		    StreamTransform result = new VerticalCutTransform(xPivot-x1);
		    // recurse left and right, adding transforms as post-ops
		    result.addSucc(traceback(partitions, curPartition, x1, xPivot, y1, y2, tPivot));
		    // mark that we have a partition here
		    curPartition = new PartitionRecord();
		    partitions.add(curPartition);
		    result.addSucc(traceback(partitions, curPartition, xPivot+1, x2, y1, y2, tileLimit-tPivot));
		    // all done
		    return result;
		}
	    }
	}

	// otherwise, see if we made a horizontal cut (breaking into top/bottom pieces)
	for (int yPivot=y1; yPivot<y2; yPivot++) {
	    for (int tPivot=1; tPivot<tileLimit; tPivot++) {
		int cost = Math.max(get(x1, x2, y1, yPivot, tPivot),
				    get(x1, x2, yPivot+1, y2, tileLimit-tPivot));
		if (cost==A[x1][x2][y1][y2][tileLimit]) {
		    // there's a division at this <yPivot>.  We'll
		    // return a horizontal cut.
		    //System.err.println("tracing horizontal cut at y=" + yPivot + " from x=" + x1 + " to x=" + x2 + " in " + cont.getName());
		    StreamTransform result = new HorizontalCutTransform(yPivot-y1);
		    // recurse left and right, adding transforms as post-ops
		    result.addSucc(traceback(partitions, curPartition, x1, x2, y1, yPivot, tPivot));
		    // mark that we have a partition here
		    curPartition = new PartitionRecord();
		    partitions.add(curPartition);
		    result.addSucc(traceback(partitions, curPartition, x1, x2, yPivot+1, y2, tileLimit-tPivot));
		    // all done
		    return result;
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
