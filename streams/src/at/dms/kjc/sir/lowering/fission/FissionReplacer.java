package at.dms.kjc.sir.lowering.fission;

import java.util.*;
import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;
import at.dms.compiler.*;

public class FissionReplacer extends LinearReplacer {
    /**
     * Max amount to fiss any filter by.
     */
    private int maxFissAmount;

    /**
     * Only create these internally.
     */
    private FissionReplacer(int maxFissAmount) {
	this.maxFissAmount = maxFissAmount;
    }

    /**
     * Fisses all eligible filters in <str> as much as possible, with
     * each fission expansion not exceeding <maxFissAmount>.
     */
    public static void doit(SIRStream str, int maxFissAmount) {
	IterFactory.createFactory().createIter(str).accept(new FissionReplacer(maxFissAmount));
    }

    /**
     * Overrides replacement method in LinearReplacer.
     */
    public boolean makeReplacement(SIRStream self) {
	// only deal with filters
	if (!(self instanceof SIRFilter)) {
	    return false;
	}
	SIRFilter filter = (SIRFilter)self;
	
	int filterMax = VerticalFission.getMaxFiss(filter);

	// don't do anything if can't split
	if (filterMax<2) {
	    return false;
	}

	// otherwise, fiss by min of <filterMax> and <maxFissAmount>
	SIRPipeline fissed = VerticalFission.fiss(filter, Math.min(filterMax, maxFissAmount));

	// replace in parent
	filter.getParent().replace(filter, fissed);
	return true;
    }
}
