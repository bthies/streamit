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
     * A_s[x1][x2][y1][y2][n][j] holds minimum cost of assigning
     * children (x1..x2, y1..y2) of stream s to n tiles.  <j> is 1 if
     * these children are next to a downstream joiner in the current
     * configuration; <j> is zero otherwise.  If this corresponds to a
     * filter's config, then A is null.
     */
    protected int[][][][][][] A;

    /**
     * The partitioner this is part of.
     */
    protected DynamicProgPartitioner partitioner;

    protected DPConfig(DynamicProgPartitioner partitioner) {
	this.partitioner = partitioner;
    }

    /**
     * Return the bottleneck work if this config is fit on <tileLimit>
     * tiles.  <nextToJoiner> is 1 iff this is next to a downstream
     * joiner under the current arrangement.
     */
    abstract protected int get(int tileLimit, int nextToJoiner);

    /**
     * Traceback through a pre-computed optimal solution, keeping
     * track of new partitions in <partitions> and adding to
     * current partition <curPartition>, and returning stream
     * transform to perform best partitioning.
     */
    abstract public StreamTransform traceback(LinkedList partitions, PartitionRecord curPartition, int tileLimit, int nextToJoiner);

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

