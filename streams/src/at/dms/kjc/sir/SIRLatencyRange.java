package at.dms.kjc.sir;

/** 
 * This represents a range of latencies for message delivery.
 */
public class SIRLatencyRange extends SIRLatencyMax {
    /**
     * The minimum latency for this.  (The maximum latency is held in
     * the superclass.)
     */
    protected final int min;
    
    /**
     * Constructs a latency with the range between <min> and <max>.
     */
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
