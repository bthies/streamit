package at.dms.kjc.sir;

/** 
 * This represents a maximum latency for message delivery.
 */
class SIRLatencyMax extends SIRLatency implements Comparable {
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
}

