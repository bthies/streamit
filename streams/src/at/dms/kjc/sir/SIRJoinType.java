package at.dms.kjc.sir;

import at.dms.kjc.*;

/**
 * This class enumerates the types of joiners.
 */
public class SIRJoinType {
    /**
     * A duplicating splitter.
     */
    public static final SIRJoinType COMBINE = new SIRJoinType();
    /**
     * An equal-weight round robing splitter.
     */
    public static final SIRJoinType ROUND_ROBIN = new SIRJoinType();
    /**
     * A round robin splitter with individual weights for each tape.
     */
    public static final SIRJoinType WEIGHTED_RR = new SIRJoinType();
    /**
     * A null splitter, providing no tokens on its output.
     */
    public static final SIRJoinType NULL = new SIRJoinType();
    /**
     * Constructs a split type.
     */
    private SIRJoinType() {}
}
