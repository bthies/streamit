package at.dms.kjc.sir.lowering.partition.linear;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.sir.linear.frequency.*;
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
	int savings;

	switch(collapse) {

	case LinearPartitioner.COLLAPSE_ANY: {
	    // if we still have flexibility, do better out of
	    // collapsing or not
	    savings = Math.max(get(LinearPartitioner.COLLAPSE_LINEAR),
			       Math.max(get(LinearPartitioner.COLLAPSE_FREQ),
					get(LinearPartitioner.COLLAPSE_NONE)));
	    break;
	}
	    
	case LinearPartitioner.COLLAPSE_FREQ: {
	    savings = getFreq();
	    break;
	}

	case LinearPartitioner.COLLAPSE_LINEAR:
	case LinearPartitioner.COLLAPSE_NONE: {
	    savings = 0;
	    break;
	}

	default: {
	    savings = -1;
	    Utils.fail("Unrecognized collapse value: " + collapse);
	}
	}

	return savings;
    }

    /**
     * Returns savings of this node if converted to frequency domain.
     */
    private int getFreq() {
	LinearAnalyzer lfa = partitioner.getLinearAnalyzer();
	if (!lfa.hasLinearRepresentation(filter) || !LEETFrequencyReplacer.canReplace(filter, lfa)) {
	    return Integer.MIN_VALUE;
	} else {
	    LinearFilterRepresentation l = lfa.getLinearRepresentation(filter);
	    /*
	    System.err.println("For " + filter + " scalingfactor=" + getScalingFactor(l, filter) + 
			       " and savings = ( " + l.getCost().getDirectCost() + " (direct) - " + l.getCost().getFrequencyCost() + 
			       " (freq) ) = " + (l.getCost().getDirectCost() - l.getCost().getFrequencyCost()));
	    */
	    return getScalingFactor(l, filter) * (l.getCost().getDirectCost() - l.getCost().getFrequencyCost());
	}
    }

    public StreamTransform traceback(int collapse) {
	if (collapse==LinearPartitioner.COLLAPSE_ANY) {
	    // only way to be profitable is with FREQ, so see if we are...
	    if (getFreq()>0) {
		return traceback(LinearPartitioner.COLLAPSE_FREQ);
	    } else {
		return traceback(LinearPartitioner.COLLAPSE_NONE);
	    }
	} else if (collapse==LinearPartitioner.COLLAPSE_FREQ) {
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
