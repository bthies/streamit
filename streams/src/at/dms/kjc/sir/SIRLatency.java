package at.dms.kjc.sir;

import at.dms.kjc.*;
import java.io.Serializable;

/** 
 * This represents a latency for message delivery.  A latency can be:
 *    - best-effort delivery
 *    - a max time for delivery 
 *    - a (min,max) range for delivery
 *    - a set of discrete times for delivery
 */
public class SIRLatency implements Serializable {
    /**
     * This signifies a best-effort latency.
     */
    public static final SIRLatency BEST_EFFORT = new SIRLatency();
    
    protected SIRLatency() {}
    
    /**
     * Accepts the specified visitor.
     */
    public void accept(KjcVisitor p) {
	if (p instanceof SLIRVisitor) {
	    ((SLIRVisitor)p).visitLatency(this);
	} else {
	    at.dms.util.Utils.fail("Use SLIR visitor to visit an SIR node.");
	}
    }

    /**
     * Accepts the specified attribute visitor.
     * @param   p               the visitor
     */
    public Object accept(AttributeVisitor p) {
	if (p instanceof SLIRAttributeVisitor) {
	    return ((SLIRAttributeVisitor)p).visitLatency(this);
	} else {
	    return this;
	}
    }

}
