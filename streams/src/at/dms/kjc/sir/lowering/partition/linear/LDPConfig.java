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
	Utils.assert(strPush % linPush == 0, "Have non-integral scaling factor of " + strPush + " / " + linPush);
	// make sure ratios are same
	Utils.assert(pushRatio==popRatio, "Found unequal push and pop ratios when computing scaling factor.");
	return pushRatio;
    }

    /**
     * Prints the array of memoized values of this.
     */
    public void printArray() {
	String msg = "Printing array for " + getStream().getIdent() + " --------------------------";
	System.err.println(msg);
	for (int i1=0; i1<A.length; i1++) {
	    for (int i2=0; i2<A[0].length; i2++) {
		for (int i3=0; i3<A[0][0].length; i3++) {
		    for (int i4=0; i4<A[0][0][0].length; i4++) {
			System.err.println();
			for (int i5=0; i5<4; i5++) {
			    System.err.println(getStream().getIdent() + "[" + i1 + "][" + i2 + "][" + i3 + "][" + i4 + "][" + 
					       LinearPartitioner.COLLAPSE_STRING(i5) + "] = " + A[i1][i2][i3][i4][i5]);
			}
		    }
		}
	    }
	}
	for (int i=0; i<msg.length(); i++) {
	    System.err.print("-");
	}
	System.err.println();
    }
}

