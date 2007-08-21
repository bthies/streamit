package at.dms.kjc.backendSupport;

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
    

    /**
     * to determine if INIT || PRIMEPUMP.
     */
    public static boolean isInitOrPrimepump(SchedulingPhase whichPhase) {
        return whichPhase.equals(INIT) || whichPhase.equals(PRIMEPUMP);
    }
}

