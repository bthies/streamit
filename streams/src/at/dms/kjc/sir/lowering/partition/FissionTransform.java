package at.dms.kjc.sir.lowering.partition;

import java.util.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fission.*;

/**
 * Represents a fission of a filter in a stream graph.
 */

public class FissionTransform extends StreamTransform {
    /**
     * The number of ways to fiss.
     */
    private int reps;

    public FissionTransform(int reps) {
	this.reps = reps;
    }

    /**
     * Perform the transform on <str> and return new stream.
     */
    protected SIRStream doMyTransform(SIRStream str) {
	// make sure we're fissable
	Utils.assert((str instanceof SIRFilter) && StatelessDuplicate.isFissable((SIRFilter)str),
		     "Didn't get a filter or it wasn't fissable: " + str);
	System.err.println("trying to split " + str + " " + reps + " ways.");
	return StatelessDuplicate.doit((SIRFilter)str, reps);
    }

    public String toString() {
	return "Fission transform (" + reps + " ways)";
    }

}
