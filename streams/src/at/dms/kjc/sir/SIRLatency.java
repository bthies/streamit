package at.dms.kjc.sir;

/** 
 * This represents a latency for message delivery.  A latency can be:
 *    - best-effort delivery
 *    - a max time for delivery 
 *    - a (min,max) range for delivery
 *    - a set of discrete times for delivery
 */
public class SIRLatency {
    /**
     * This signifies a best-effort latency.
     */
    public static final SIRLatency BEST_EFFORT = new SIRLatency();
    
    protected SIRLatency() {}
}
