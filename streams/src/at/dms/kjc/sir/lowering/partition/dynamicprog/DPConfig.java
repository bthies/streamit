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

abstract class DPConfig implements Cloneable {
    /**  
     * A[i,j,k] that gives the bottleneck work for segment i-j of
     * the structure if children i through j are assigned to k
     * tiles.  If this corresponds to a filter's config, then A is
     * null.
     */
    protected int[][][] A;

    /**
     * The partitioner this is part of.
     */
    protected DynamicProgPartitioner partitioner;

    protected DPConfig(DynamicProgPartitioner partitioner) {
	this.partitioner = partitioner;
    }

    /**
     * Return the bottleneck work if this config is fit on
     * <tileLimit> tiles.
     */
    abstract protected int get(int tileLimit);

    /**
     * Traceback through a pre-computed optimal solution, keeping
     * track of new partitions in <partitions> and adding to
     * current partition <curPartition>, and returning stream
     * transform to perform best partitioning.  <tileCounter> is a
     * one-element array holding the value of the current tile we
     * are assigning to filters (in a depth-first way).
     */
    abstract public StreamTransform traceback(LinkedList partitions, PartitionRecord curPartition, int tileLimit);

    /**
     * Returns the stream this config is wrapping.
     */
    abstract public SIRStream getStream();

    /**
     * Returns a copy of this with the same A matrix as this
     * (object identity is the same), but with <str> as the
     * stream.
     */
    public DPConfig copyWithStream(SIRStream str) {
	// use cloning instead of a new constructor so that we
	// don't reconstruct a fresh A array.
	DPConfig result = null;
	try {
	    result = (DPConfig)this.clone();
	} catch (CloneNotSupportedException e) {
	    e.printStackTrace();
	}
	result.setStream(str);
	return result;
    }

    /**
     * Sets this to wrap <str>.
     */
    protected abstract void setStream(SIRStream str);
	
}

