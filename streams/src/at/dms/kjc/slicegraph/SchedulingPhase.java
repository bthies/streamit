package at.dms.kjc.slicegraph;

/** What phase of Slice graph scheduling is a bit of code generation associated with? 
 * This enum should tell: initialization, pump priming for software pipelining, or steady state. 
 */
public enum SchedulingPhase {
    /** Indicates scheduling for pre-initialization phase */
    PREINIT,
    /** Indicates scheduling for initialization phase */
    INIT,
    /** indicates scheduling for "prime pump" phase in a software pipelined schedule */
    PRIMEPUMP, 
    /** indicates schdulting for the steady state */
    STEADY;
    
    public static boolean isSteady(SchedulingPhase phase) {
        return phase == STEADY;
    }
    
    /**
     * to determine if INIT || PRIMEPUMP.
     */
    public static boolean isInitOrPrimepump(SchedulingPhase whichPhase) {
        return whichPhase.equals(INIT) || whichPhase.equals(PRIMEPUMP);
    }
}

