package at.dms.kjc.sir.lowering.partition;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.sir.*;
import at.dms.compiler.*;
import java.util.*;

class WorkInfo {
    /**
     * Mapping of all workinfo's that have been created to date.  Maps
     * workinfo's to an integer with their -measured- work.
     */
    private static final HashMap measured = new HashMap();
    /**
     * Number of times this node executes.  This will be 1 for
     * containers, but the actual number of executions for filters.
     */
    private int reps;
    /**
     * Original, estimated work per execution.
     */
    private int workEstimate;
    /**
     * Exact work per execution.
     */
    private int workExact;
    /**
     * Stream this is for.
     */
    private SIRStream str;
    
    private WorkInfo(SIRStream str, int reps, int workEstimate) {
	this.str = str;
	this.reps = reps;
	this.workEstimate = workEstimate;
    }

    public static WorkInfo create(SIRFilter filter, int reps, int workEstimate) {
	// make fresh node
	WorkInfo result = new WorkInfo(filter, reps, workEstimate);
	// get measured execution time
	Object exact  = measured.get(result);
	if (exact!=null) {
	    // if we calculated it before, reuse it 
	    result.workExact = ((Integer)exact).intValue();
	} else {
	    // otherwise, calculate exact work
	    if (KjcOptions.simulatework) {
		// if simulation is enabled, then call raw simluator
		result.workExact = RawWorkEstimator.estimateWork(filter); 
	    } else {
		// otherwise, just take workEstimate to be exact
		result.workExact = workEstimate;
	    }
	    measured.put(result, new Integer(result.workExact));
	}
	return result;
    }

    public static WorkInfo create(SIRContainer cont, int workEstimate) {
	// we don't measure work for container, so just make a node
	// with the estimate
	WorkInfo result = new WorkInfo(cont, 1, workEstimate);
	result.workExact = workEstimate;
	return result;
    }

    /**
     * Returns the amount of work.
     */
    public String toString() {
	return "" + (reps*workExact);
    }

    /**
     * Work per steady-state execution of graph.
     */
    public int getTotalWork() {
	return reps*workExact;
    }

    /**
     * Returns exact work for one execution.
     */
    public int getUnitWork() {
	return workExact;
    }

    /**
     * Returns original, inexact estimate of work.  Mostly for
     * statistics gathering on how bad the original estimate was.
     */
    public int getInexactUnitWork() {
	return workEstimate;
    }
    
    public int getReps() {
	return reps;
    }

    /**
     * Only intended for incrementing container work.
     */
    public void incrementWork(int work) {
	assert str instanceof SIRContainer;
	this.workExact += work;
    }

    /**
     * Make them hash to the same node if they started from the same
     * filter and have the same work *estimate*
     */
    public int hashCode() {
	return workEstimate;
    }

    /**
     * Count as equal if started from same filter and have same work
     * estimate.
     */
    public boolean equals(Object o) {
	return (o instanceof WorkInfo &&
		((WorkInfo)o).workEstimate==this.workEstimate);
    }

}

