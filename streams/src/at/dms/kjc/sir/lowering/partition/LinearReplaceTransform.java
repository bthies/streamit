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
	if (str instanceof SIRFilter) {
	    // if we just have a filter, figure that the custom
	    // implementation of the filter is going to be better than
	    // our own version of matrix multiply... that is, do
	    // nothing.
	} else {
	    // again detect that <str> is linear, since it is a newly constructed stream
	    LinearAnalyzer.findLinearFilters(str, KjcOptions.debug, lfa);
	    // otherwise, look at the number of multiplies, and do an
	    // unrolled direct replacement only if it's less than our
	    // threshoold
	    if (lfa.getLinearRepresentation(str).getCost().getMultiplies()<=LinearPartitioner.MAX_MULT_TO_UNROLL) {
		LinearDirectReplacer.doReplace(lfa, str);
	    } else {
		if (KjcOptions.atlas) {
		    LinearAtlasReplacer.doReplace(lfa, str);
		} else {
		    LinearDiagonalReplacer.doReplace(lfa, str);
		}
	    } /* TODO: else if sparse matrix {
		 LinearIndirectReplacer.doReplace(lfa, str);
		 }
	      */
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
