package at.dms.kjc.sir.lowering.partition.linear;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.partition.*;

abstract class LDPConfig implements Cloneable {
    /**
     * The partitioner this is part of.
     */
    protected LinearPartitioner partitioner;
    /**
     * Number of partitions assigned so far, during traceback... just
     * used for dot output, stored here for convenience.
     */
    public static int numAssigned;
    /**
     * Mapping from filter to partition number, just for convenience
     * in traceback.
     */
    public static HashMap partitions;

    protected LDPConfig(LinearPartitioner partitioner) {
	this.partitioner = partitioner;
    }

    /**
     * Return the savings given collapse policy <collapsed>.
     */
    abstract protected long get(int collapse);

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

    /**
     * Given that <l> is a linear representation for <str>, returns
     * how many times <l> should be executed to produce the same
     * number of items that are produced in a steady-state execution
     * of <str>.
     */
    protected int getScalingFactor(LinearFilterRepresentation l, SIRStream str) {
	int strPush = str.getPushForSchedule(partitioner.getExecutionCounts());
	int strPop = str.getPopForSchedule(partitioner.getExecutionCounts());
	int linPush = l.getPushCount();
	int linPop = l.getPopCount();
	
	int pushRatio = strPush / linPush;
	int popRatio = strPop / linPop;
	// see if we can get away with this being an integer
	assert strPush % linPush == 0:
            "Have non-integral scaling factor of " +
            strPush + " / " + linPush + " for stream " + str.getName();
	// make sure ratios are same
	assert pushRatio==popRatio:
            "Found unequal push and pop ratios when computing scaling factor" +
            "\n  str=" + str +
            "\n  strPush=" + strPush +
            "\n  strPop=" + strPush +
            "\n  linPush=" + linPush +
            "\n  linPop=" + linPop;
	assert pushRatio>0:
            "Found non-positive scaling factor:" + pushRatio;
	return pushRatio;
    }
}

