package at.dms.kjc.sir.lowering;

import streamit.scheduler2.base.StreamInterface;
import streamit.scheduler2.ScheduleBuffers;

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
    private ScheduleBuffers buffers;

    public SIRSchedule(SIRStream toplevel, StreamInterface schedInterface) {
	buffers = new ScheduleBuffers(IterFactory.createIter(toplevel));
	buffers.computeBuffersFor(schedInterface.getInitSchedule());
	buffers.computeBuffersFor(schedInterface.getSteadySchedule());
    }

    /**
     * Returns buffer size between <op1> and <op2> using this
     * schedule.
     */
    public int getBufferSizeBetween(SIROperator op1, SIROperator op2) {
	// shouldn't have two splitters or joiners
	Utils.assert(op1 instanceof SIRStream || op2 instanceof SIRStream);
	// if we have a splitter or joiner, the scheduler interface
	// takes its parent for this function call
	if (op1 instanceof SIRSplitter || op1 instanceof SIRJoiner) {
	    op1 = op1.getParent();
	}
	if (op2 instanceof SIRSplitter || op2 instanceof SIRJoiner) {
	    op2 = op1.getParent();
	}
	// create iterators
	SIRIterator iter1 = IterFactory.createIter((SIRStream)op1);
	SIRIterator iter2 = IterFactory.createIter((SIRStream)op2);
	// call scheduler
	return buffers.getBufferSizeBetween(iter1, iter2);
    }

}
