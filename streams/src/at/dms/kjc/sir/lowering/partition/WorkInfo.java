package at.dms.kjc.sir.lowering.partition;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.compiler.*;
import java.util.*;

class WorkInfo {
    /**
     * Number of times this node executes.  This will be 1 for
     * containers, but the actual number of executions for filters.
     */
    private int reps;
    /**
     * Work per execution.
     */
    private int work;

    public WorkInfo(int reps, int work) {
	this.reps = reps;
	this.work = work;
    }

    /**
     * Returns the amount of work.
     */
    public String toString() {
	return "" + (reps*work);
    }

    /**
     * Work per steady-state execution of graph.
     */
    public int getTotalWork() {
	return reps*work;
    }

    /**
     * Work for one execution.
     */
    public int getUnitWork() {
	return work;
    }

    public int getReps() {
	return reps;
    }

    /**
     * Only intended for incrementing container work, where number of
     * executions is 1.
     */
    public void incrementWork(int work) {
	Utils.assert(reps==1);
	this.work += work;
    }
}

