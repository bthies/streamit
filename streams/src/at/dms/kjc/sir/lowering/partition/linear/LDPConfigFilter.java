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

class LDPConfigFilter extends LDPConfig {
    /**
     * The filter corresponding to this.
     */
    private SIRFilter filter;
	
    public LDPConfigFilter(SIRFilter filter, LinearPartitioner partitioner) {
	super(partitioner);
	this.filter = filter;
	this.A = null;
    }

    public int get(int collapse) {
	// never save anything at the filter level unless you're going
	// to frequency
	if (collapse==LinearPartitioner.COLLAPSE_FREQ) {
	    LinearCost cost = partitioner.getLinearAnalyzer().getLinearRepresentation(filter).getCost();
	    return cost.getFrequencyCost() - cost.getDirectCost();
	} else {
	    return 0;
	}
    }

    public StreamTransform traceback(int collapse) {
	if (collapse==LinearPartitioner.COLLAPSE_FREQ) {
	    return new FreqReplaceTransform(partitioner.getLinearAnalyzer());
	} else if (collapse==LinearPartitioner.COLLAPSE_LINEAR) {
	    return new LinearReplaceTransform(partitioner.getLinearAnalyzer());
	} else {
	    return new IdentityTransform();
	}
    }

    public SIRStream getStream() {
	return filter;
    }

    /**
     * Requires <str> is a filter.
     */
    protected void setStream(SIRStream str) {
	Utils.assert(str instanceof SIRFilter);
	this.filter = (SIRFilter)str;
    }
}
