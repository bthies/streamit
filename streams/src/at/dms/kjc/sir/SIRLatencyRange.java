package at.dms.kjc.sir;

import at.dms.kjc.*;

/** 
 * This represents a range of latencies for message delivery.
 */
public class SIRLatencyRange extends SIRLatencyMax {
    /**
     * The minimum latency for this.  (The maximum latency is held in
     * the superclass.)
     */
    protected int min;
    
    /**
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    protected SIRLatencyRange() {
	super();
    }
    
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

    /**
     * Accepts the specified attribute visitor.
     * @param   p               the visitor
     */
    public Object accept(AttributeVisitor p) {
	if (p instanceof SLIRAttributeVisitor) {
	    return ((SLIRAttributeVisitor)p).visitLatencyRange(this);
	} else {
	    return this;
	}
    }

    /**
     * Accepts the specified visitor.
     */
    public void accept(KjcVisitor p) {
	if (p instanceof SLIRVisitor) {
	    ((SLIRVisitor)p).visitLatencyRange(this);
	} else {
	    at.dms.util.Utils.fail("Use SLIR visitor to visit an SIR node.");
	}
    }
}
