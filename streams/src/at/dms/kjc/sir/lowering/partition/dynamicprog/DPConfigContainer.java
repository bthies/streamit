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

class DPConfigContainer extends DPConfig {
    /**
     * The stream for this container.
     */
    protected SIRContainer cont;

    public DPConfigContainer(SIRContainer cont, DynamicProgPartitioner partitioner) {
	super(partitioner);
	this.cont = cont;
	this.A = new int[cont.size()][cont.size()][partitioner.getNumTiles()+1];
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
	return get(0, cont.size()-1, tileLimit);
    }

    protected int get(int child1, int child2, int tileLimit) {
	// if we've memoized the value before, return it
	if (A[child1][child2][tileLimit]>0) {
	    /*
	      System.err.println("Found memoized A[" + child1 + "][" + child2 + "][" + tileLimit + "] = " + 
	      A[child1][child2][tileLimit] + " for " + cont.getName());
	    */
	    return A[child1][child2][tileLimit];
	}

	// if we are down to one child, then descend into child
	if (child1==child2) {
	    int childCost = childConfig(child1).get(tileLimit);
	    A[child1][child2][tileLimit] = childCost;
	    //System.err.println("Returning " + childCost + " from descent into child.");
	    return childCost;
	}	    

	// otherwise, if <tileLimit> is 1, then just sum the work
	// of our components
	if (tileLimit==1) {
	    int sum = get(child1, child1, tileLimit) + get(child1+1, child2, tileLimit);
	    A[child1][child2][tileLimit] = sum;
	    //System.err.println("Returning sum " + sum + " from fusion.");
	    return sum;
	}

	// otherwise, find the lowest-cost child AFTER WHICH to
	// make a partition at this level
	/*
	  System.err.println("Allocating " + tileLimit + " between children " + child1 + " and " + child2 + 
	  " of " + cont.getClass() + " " + cont.getName());
	*/
	int min = Integer.MAX_VALUE;
	for (int i=child1; i<child2; i++) {
	    for (int j=1; j<tileLimit; j++) {
		int cost = Math.max(get(child1, i, j), get(i+1, child2, tileLimit-j));
		if (cost < min) {
		    /*
		      System.err.println(" found new min of " + cost + " for " + cont.getName() + " [" 
		      + child1 + "," + child2 + "] with " + tileLimit + " tiles:\n" +
		      "\t" + j + " tiles to children " + child1 + "-" + i + "\n" + 
		      "\t" + (tileLimit-j) + " tiles to children " + (i+1) + "-" + 
		      child2);
		    */
		    min = cost;
		}
	    }
	}
	/*
	  System.err.println("Assigning MIN A[" + child1 + "][" + child2 + "][" + tileLimit + "]=" + min + 
	  " for " + cont.getName());
	*/
	A[child1][child2][tileLimit] = min;
	return min;
    }

    /**
     * Traceback function.
     */
    public StreamTransform traceback(LinkedList partitions, PartitionRecord curPartition, int tileLimit) {
	// only support fusion for now.
	FusionTransform st = new FusionTransform();
	// add partitions at beginning and end
	st.addPartition(0);
	st.addPartition(cont.size());

	traceback(st, partitions, curPartition, 0, cont.size()-1, tileLimit);
	// if the whole container is assigned to one tile, record
	// it as such.
	if (tileLimit==1) {
	    curPartition.add(cont);
	} 
	return st;
    }
	
    /**
     * Traceback helper function. The child1, child2, and
     * tileLimit are as above.  The stream transform <st> is the
     * one we're building up to carry out the final partitioning.
     */
    protected void traceback(FusionTransform st, LinkedList partitions, PartitionRecord curPartition,
			     int child1, int child2, int tileLimit) {
	// if we only have one tile left, or if we are only
	// looking at one child, then just recurse into children
	if (child1==child2 || tileLimit==1) {
	    for (int i=child1; i<=child2; i++) {
		st.add(childConfig(i).traceback(partitions, curPartition, tileLimit));
	    }
	    return;
	}

	// otherwise, find the best partitioning of this into
	// <tileLimit> sizes.  See where the first break was,
	// breaking ties by fewest number of tiles required.
	int min = Integer.MAX_VALUE;
	for (int i=child1; i<child2; i++) {
	    for (int j=1; j<tileLimit; j++) {
		int cost = Math.max(get(child1, i, j), get(i+1, child2, tileLimit-j));
		if (cost==A[child1][child2][tileLimit]) {
		    /*
		      System.err.println("Found best split of " + cont.getName() + " [" + child1 + "," + child2 + 
		      "]: " + j + " to [" + child1 + "," + i + "],  " + (tileLimit-j) + " to [" +
		      (i+1) + "," + child2 + "]");
		    */
		    // if we found our best cost, then the
		    // division is at this <i> with <j> partitions
		    // on the left.  First recurse left, then
		    // start new partition, then recurse right.
		    traceback(st, partitions, curPartition, child1, i, j);
		    curPartition = new PartitionRecord();
		    partitions.add(curPartition);
		    traceback(st, partitions, curPartition, i+1, child2, tileLimit-j);
		    // remember that we had a partition here
		    st.addPartition(i+1);
		    return;
		}
	    }
	}	    
    }
	
    /**
     * Returns config for child at index <childIndex>
     */
    protected DPConfig childConfig(int childIndex) {
	return partitioner.getConfig(cont.get(childIndex));
    }
}
