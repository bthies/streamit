package at.dms.kjc.sir.lowering;

import streamit.scheduler2.Scheduler;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.util.Utils;
import at.dms.kjc.iterator.*;

/**
 * This provides an SIR wrapper for the underlying schedule (from the
 * scheduler package).  Right now it is used only to keep track of
 * buffer sizes required by a schedule.
 */
public class SIRSchedule  {
    /**
     * The scheduler interface for buffer sizes.
     */
    private Scheduler scheduler;

    public SIRSchedule(SIRStream toplevel, Scheduler scheduler) {
        this.scheduler = scheduler;
	scheduler.computeBufferUse();
    }

    /**
     * Returns buffer size between <op1> and <op2> using this
     * schedule.
     */
    public int getBufferSizeBetween(SIROperator op1, SIROperator op2) {
	// shouldn't have two splitters or joiners
	assert op1 instanceof SIRStream || op2 instanceof SIRStream;
	// if we have a splitter or joiner, the scheduler interface
	// takes its parent for this function call
	if (op1 instanceof SIRSplitter || op1 instanceof SIRJoiner) {
	    op1 = op1.getParent();
	}
	if (op2 instanceof SIRSplitter || op2 instanceof SIRJoiner) {
	    op2 = op2.getParent();
	}
	// create iterators
	SIRIterator iter1 = IterFactory.createFactory().createIter((SIRStream)op1);
	SIRIterator iter2 = IterFactory.createFactory().createIter((SIRStream)op2);
	// call scheduler
	//System.err.println("Looking for schedule between " + op1 + " and " + op2);
	return scheduler.getBufferSizeBetween(iter1, iter2);
    }

}
