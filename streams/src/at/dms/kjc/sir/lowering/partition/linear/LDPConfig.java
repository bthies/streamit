package at.dms.kjc.sir.lowering.partition.linear;

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

abstract class LDPConfig implements Cloneable {
    /**  
     * A_s[x1][x2][y1][y2][c] holds savings if children (x1..x2,
     * y1..y2) of stream s given collapse policy <c>. If this
     * corresponds to a filter's config, then A is null.
     */
    protected int[][][][][] A;

    /**
     * The partitioner this is part of.
     */
    protected LinearPartitioner partitioner;

    protected LDPConfig(LinearPartitioner partitioner) {
	this.partitioner = partitioner;
    }

    /**
     * Return the savings given collapse policy <collapsed>.
     */
    abstract protected int get(int collapse);

    /**
     * Traceback through a pre-computed optimal solution.
     */
    abstract public StreamTransform traceback(int collapse);

    /**
     * Returns the stream this config is wrapping.
     */
    abstract public SIRStream getStream();

    /**
     * Returns a copy of this with the same A matrix as this
     * (object identity is the same), but with <str> as the
     * stream.
     */
    public LDPConfig copyWithStream(SIRStream str) {
	// use cloning instead of a new constructor so that we
	// don't reconstruct a fresh A array.
	LDPConfig result = null;
	try {
	    result = (LDPConfig)this.clone();
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

