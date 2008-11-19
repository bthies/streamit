package at.dms.kjc.sir.lowering.partition;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.spacedynamic.*;
import at.dms.kjc.sir.*;
import at.dms.compiler.*;
import java.util.*;

class WorkInfo {
    /**
     * Mapping of all workinfo's that have been created to date.  Maps
     * workinfo's to a long with their -measured- work.
     */
    private static final HashMap<WorkInfo, Long> measured = new HashMap<WorkInfo, Long>();
    /**
     * Number of times this node executes.  This will be 1 for
     * containers, but the actual number of executions for filters.
     */
    private int reps;
    /**
     * Original, estimated work per execution.
     */
    private long workEstimate;
    /**
     * Exact work per execution.
     */
    private long workExact;
    /**
     * Stream this is for.
     */
    private SIRStream str;
    
    private WorkInfo(SIRStream str, int reps, long workEstimate) {
        this.str = str;
        this.reps = reps;
        this.workEstimate = workEstimate;
    }

    public static WorkInfo create(SIRFilter filter, int reps, long workEstimate) {
        // make fresh node
        WorkInfo result = new WorkInfo(filter, reps, workEstimate);
        // get measured execution time
        Object exact  = measured.get(result);
        if (exact!=null) {
            // if we calculated it before, reuse it 
            result.workExact = ((Long)exact).longValue();
        } else {
            // otherwise, calculate exact work
            if (KjcOptions.simulatework) {
                // if simulation is enabled, then call raw simluator, call the correct work estimation pass
                if (KjcOptions.spacedynamic || KjcOptions.spacetime)
                    result.workExact = at.dms.kjc.spacedynamic.RawWorkEstimator.estimateWork(filter); 
                else
                    
                    result.workExact = at.dms.kjc.raw.RawWorkEstimator.estimateWork(filter); 
            } else {
                // otherwise, just take workEstimate to be exact
                result.workExact = workEstimate;
            }
            measured.put(result, new Long(result.workExact));
        }
        return result;
    }

    public static WorkInfo create(SIRContainer cont, long workEstimate) {
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
    public long getTotalWork() {
        return reps*workExact;
    }

    /**
     * Returns exact work for one execution.
     */
    public long getUnitWork() {
        return workExact;
    }

    /**
     * Returns original, inexact estimate of work.  Mostly for
     * statistics gathering on how bad the original estimate was.
     */
    public long getInexactUnitWork() {
        return workEstimate;
    }
    
    public int getReps() {
        return reps;
    }

    /**
     * Only intended for incrementing container work.
     */
    public void incrementWork(long work) {
        assert str instanceof SIRContainer;
        this.workExact += work;
    }

    /**
     * Make them hash to the same node if they started from the same
     * filter and have the same work *estimate*
     */
    public int hashCode() {
        return (int)workEstimate;
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

