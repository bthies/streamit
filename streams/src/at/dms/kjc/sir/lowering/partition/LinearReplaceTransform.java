package at.dms.kjc.sir.lowering.partition;

import java.util.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.linear.LinearPartitioner;
import at.dms.kjc.sir.lowering.fusion.*;

/**
 * LinearReplace transform on a stream graph.
 */

public final class LinearReplaceTransform extends StreamTransform {
    /**
     * Linear analyzer used to construct this.
     */
    private LinearAnalyzer lfa;

    public LinearReplaceTransform(LinearAnalyzer lfa) {
	super();
	this.lfa = lfa;
    }

    /**
     * Perform the transform on <str> and return new stream.
     */
    public SIRStream doMyTransform(SIRStream str) {
	// again detect that <str> is linear, since it is a newly constructed stream
	LinearAnalyzer.findLinearFilters(str, KjcOptions.debug, lfa);
	LinearFilterRepresentation linearRep = lfa.getLinearRepresentation(str);
	boolean largeCode = linearRep.getCost().getMultiplies() >= LinearPartitioner.MAX_MULT_TO_UNROLL;
	// use atlas if all of the following are true: 1) atlas option
	// is enabled, 2) there is largeCode, 3) push>=2 (since
	// otherwise, presumably, atlas can't improve anything)
	if (KjcOptions.atlas && largeCode && linearRep.getPushCount()>=2) {
	    LinearAtlasReplacer.doReplace(lfa, str);
	} else {
	    // otherwise, choose between our matrix multiplies..
	    if (!largeCode) {
		// always use dirct unrolling for small containers
		LinearDirectReplacer.doReplace(lfa, str);
	    } else {
		// otherwise use diagonal by default, but indirect
		// or direct if the option is enabled
		if (KjcOptions.linearreplacement) {
		    LinearDirectReplacer.doReplace(lfa, str);
		} else if (KjcOptions.linearreplacement2) {
		    LinearIndirectReplacer.doReplace(lfa, str);
		} else {
		    // default is same as KjcOptions.linearreplacement3
		    LinearDiagonalReplacer.doReplace(lfa, str);
		}
	    }
	}
	// kind of hard to get a handle on the new stream... return
	// null for now; this shouldn't get dereferenced in linear
	// partitioner
	return null;
    }

    public String toString() {
	return "LinearReplace Transform";
    }

}
