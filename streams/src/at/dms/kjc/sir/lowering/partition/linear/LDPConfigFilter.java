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
    /**
     * Memoized values for this.
     */
    private int[] A;
    
    public LDPConfigFilter(SIRFilter filter, LinearPartitioner partitioner) {
	super(partitioner);
	this.filter = filter;
	this.A = new int[4];
	for (int i=0; i<A.length; i++) {
	    A[i] = -1;
	}
    }

    public int get(int collapse) {
	// return memoized value if we have it
	if (A[collapse]!=-1) {
	    return A[collapse];
	}

	// otherwise calculate cost...
	int cost;
	
	LinearAnalyzer lfa = partitioner.getLinearAnalyzer();
	// we don't worry about counting cost of non-linear nodes
	if (!lfa.hasLinearRepresentation(filter)) {
	    cost = 0;
	} else {
	    // otherwise get the representation
	    LinearFilterRepresentation linearRep = lfa.getLinearRepresentation(filter);
	    
	    switch(collapse) {
		
	    case LinearPartitioner.COLLAPSE_ANY: {
		// if we still have flexibility, do better out of
		// collapsing or not
		cost = Math.min(get(LinearPartitioner.COLLAPSE_LINEAR),
				Math.min(get(LinearPartitioner.COLLAPSE_FREQ),
					 get(LinearPartitioner.COLLAPSE_NONE)));
		break;
	    }
		
	    case LinearPartitioner.COLLAPSE_FREQ: {
		if (!LEETFrequencyReplacer.canReplace(filter, lfa)) {
		    cost = Integer.MAX_VALUE;
		} else {
		    cost = getScalingFactor(linearRep, filter) * linearRep.getCost().getFrequencyCost();
		}
		break;
	    }
		
	    case LinearPartitioner.COLLAPSE_LINEAR:
	    case LinearPartitioner.COLLAPSE_NONE: {
		cost = getScalingFactor(linearRep, filter) * linearRep.getCost().getDirectCost();
		break;
	    }
		
	    default: {
		Utils.fail("Unrecognized collapse value: " + collapse);
		cost = -1;
	    }
	    }
	}

	if (LinearPartitioner.DEBUG) { System.err.println("Returning filter.get " + filter.getIdent() + "[" + LinearPartitioner.COLLAPSE_STRING(collapse) + "] = " + cost); }

	// memoize value and return it
	A[collapse] = cost;
	return cost;
    }

    public StreamTransform traceback(int collapse) {

	switch(collapse) {
	    
	case LinearPartitioner.COLLAPSE_ANY: {
	    // take min of other three options
	    int[] options = { LinearPartitioner.COLLAPSE_FREQ, 
			      LinearPartitioner.COLLAPSE_LINEAR, 
			      LinearPartitioner.COLLAPSE_NONE };
	    for (int i=0; i<options.length; i++) {
		if (A[collapse] == get(options[i])) {
		    return traceback(options[i]);
		}
	    }
	    Utils.fail("Didn't find traceback; was looking for ANY.");
	    break;
	}
	    
	case LinearPartitioner.COLLAPSE_FREQ: {
	    return new FreqReplaceTransform(partitioner.getLinearAnalyzer());
	}
	    
	case LinearPartitioner.COLLAPSE_LINEAR: {
	    return new LinearReplaceTransform(partitioner.getLinearAnalyzer());
	}

	case LinearPartitioner.COLLAPSE_NONE: {
	    return new IdentityTransform();
	}
	}

	Utils.fail("Couldn't find traceback for option: " + collapse + " for " + filter);
	return null;
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
