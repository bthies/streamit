package at.dms.kjc.sir;

import at.dms.kjc.*;

/** 
 * This represents a maximum latency for message delivery.
 */
public class SIRLatencyMax extends SIRLatency implements Comparable {
    /**
     * The maximum latency.
     */
    protected final int max;

    /**
     * Constructs a new latency with the given maximum.
     */
    public SIRLatencyMax(int max) {
	this.max = max;
    }

    /**
     * Returns the maximum of this latency.
     */
    public int getMax() {
	return max;
    }

    /**
     * Compares the max time of this to that of <x>, returning the
     * difference.
     */
    public int compareTo(Object o) {
	return max - ((SIRLatencyMax)o).max;
    }

    /**
     * Accepts the specified visitor.
     */
    public void accept(KjcVisitor p) {
	if (p instanceof SLIRVisitor) {
	    ((SLIRVisitor)p).visitLatencyMax(this);
	} else {
	    at.dms.util.Utils.fail("Use SLIR visitor to visit an SIR node.");
	}
    }
}

