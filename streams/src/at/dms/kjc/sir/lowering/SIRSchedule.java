package at.dms.kjc.sir.lowering;

import streamit.scheduler1.*;
import streamit.scheduler1.simple.*;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;

import java.math.BigInteger;

/**
 * This provides an SIR wrapper for the underlying schedule (from the
 * scheduler package).
 */
public class SIRSchedule  {
    /**
     * The schedule from scheduling package.
     */
    private Schedule schedule;

    public SIRSchedule(Schedule schedule) {
	this.schedule = schedule;
    }

    /**
     * Returns buffer size between <op1> and <op2> using this
     * schedule.
     */
    public int getBufferSizeBetween(SIROperator op1, SIROperator op2) {
	return schedule.getBufferSizeBetween(op1, op2).intValue();
    }

}
