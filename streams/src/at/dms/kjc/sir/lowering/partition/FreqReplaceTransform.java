package at.dms.kjc.sir.lowering.partition;

import java.util.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.sir.linear.frequency.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;

/**
 * FreqReplace transform on a stream graph.
 */

public final class FreqReplaceTransform extends StreamTransform {
    /**
     * Linear analyzer used to construct this.
     */
    private LinearAnalyzer lfa;

    public FreqReplaceTransform(LinearAnalyzer lfa) {
	super();
	this.lfa = lfa;
    }

    /**
     * Perform the transform on <str> and return new stream.
     */
    public SIRStream doMyTransform(SIRStream str) {
	// again detect that <str> is linear, since it is a newly constructed stream
	LinearAnalyzer.findLinearFilters(str, KjcOptions.debug, lfa);
	// see if the user has specified a replacement type.  If not,
	// just use max replacement.
	int replacementType;
	if (KjcOptions.frequencyreplacement!=-1) {
	    replacementType = KjcOptions.frequencyreplacement;
	} else {
	    replacementType = FrequencyReplacer.BEST_AVAILABLE;
	}

	StreamItDot.printGraph(str.getParent(), "debug1.dot");
	StreamItDot.printGraph(str, "debug2.dot");
	FrequencyReplacer.doReplace(lfa, str, replacementType);
	StreamItDot.printGraph(str.getParent(), "debug3.dot");
	// kind of hard to get a handle on the new stream... return
	// null for now; this shouldn't get dereferenced in linear
	// partitioner
	return null;
    }

    public String toString() {
	return "FreqReplace Transform";
    }

}
