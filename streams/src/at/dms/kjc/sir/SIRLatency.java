package at.dms.kjc.sir;

/** 
 * This represents a latency for message delivery.
 */

/*
 *    - best-effort delivery
 *    - a max time for delivery 
 *    - a (min,max) range for delivery
 *    - a set of discrete times for delivery
 *
 *    - TODO:  represent latency list as a Java collection
 */

public class SIRLatency {

    public static final SIRLatency BEST_EFFORT = new SIRLatency();

    protected SIRLatency() {}

}

class SIRLatencyList extends SIRLatency {
    private final SIRLatencyMax[] entries;

    public SIRLatencyList(SIRLatencyMax[] entries) {
	// make a copy of <entries>
	this.entries = new SIRLatencyMax[entries.length];
	for (int i=0; i<entries.length; i++) {
	    this.entries[i] = entries[i];
	}
    }

    public SIRLatencyMax getEntry() { return null; }
}

class SIRLatencyMax extends SIRLatency {
    protected final int max;

    public SIRLatencyMax(int max) {
	this.max = max;
    }

    /**
     * Returns the maximum of this latency.
     */
    public int getMax() {
	return max;
    }
}

class SIRLatencyRange extends SIRLatencyMax {
    
    protected final int min;

    public SIRLatencyRange(int min, int max) {
	super(max);
	this.min = min;
    }

    /**
     * Returns the minimum of this range.
     */
    public int getMin() {
	return min;
    }

    /**
     * Returns whether or not this latency is constrained to be an
     * exact value, without a range of zero.  
     */
    public boolean isExact() {
	return min==max;
    }
}
