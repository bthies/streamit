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
    private long[] A;
    
    public LDPConfigFilter(SIRFilter filter, LinearPartitioner partitioner) {
	super(partitioner);
	this.filter = filter;
	this.A = new long[4];
	for (int i=0; i<A.length; i++) {
	    A[i] = -1;
	}
    }

    public long get(int collapse) {
	// return memoized value if we have it
	if (A[collapse]!=-1) {
	    return A[collapse];
	}

	// otherwise calculate cost...
	long cost;
	
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
		if (!LEETFrequencyReplacer.canReplace(filter, lfa)
		    // currently don't support frequency implementation on raw
		    || KjcOptions.raw!=-1) {
		    cost = Long.MAX_VALUE;
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
	if (LinearPartitioner.DEBUG) { printArray(); }

	switch(collapse) {
	    
	case LinearPartitioner.COLLAPSE_ANY: {
	    // take min of other three options.  Do this explicitly
	    // instead of referencing A array, since the symmetry
	    // optimizations could have caused this to not be
	    // evaluated with calls to get().
	    int[] options = { LinearPartitioner.COLLAPSE_FREQ, 
			      LinearPartitioner.COLLAPSE_LINEAR, 
			      LinearPartitioner.COLLAPSE_NONE };
	    long min = Long.MAX_VALUE;
	    int minOption = -1;
	    for (int i=0; i<options.length; i++) {
		long cost = get(options[i]);
		// use <= so that we default to NONE
		if (cost<=min) {
		    min = cost;
		    minOption = i;
		}
	    }
	    Utils.assert(minOption!=-1, "Didn't find traceback; was looking for ANY, A[ANY]=" + A[LinearPartitioner.COLLAPSE_ANY] + " for " + filter.getName());
	    return traceback(options[minOption]);
	}
	    
	case LinearPartitioner.COLLAPSE_FREQ: {
	    partitions.put(filter, new Integer(numAssigned));
	    return new FreqReplaceTransform(partitioner.getLinearAnalyzer());
	}
	    
	case LinearPartitioner.COLLAPSE_LINEAR: {
	    partitions.put(filter, new Integer(numAssigned));
	    return new LinearReplaceTransform(partitioner.getLinearAnalyzer());
	}

	case LinearPartitioner.COLLAPSE_NONE: {
	    partitions.put(filter, new Integer(numAssigned));
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

    public void printArray() {
	String msg = "Printing array for " + getStream().getIdent() + " --------------------------";
	System.err.println(msg);
	for (int i1=0; i1<A.length; i1++) {
	    System.err.print(getStream().getIdent() + "[" + LinearPartitioner.COLLAPSE_STRING(i1) + "] = ");
	    if (A[i1]==Long.MAX_VALUE) {
		System.err.println("INFINITY");
	    } else {
		System.err.println(A[i1]);
	    }
	}
	for (int i=0; i<msg.length(); i++) {
	    System.err.print("-");
	}
	System.err.println();
    }
}
