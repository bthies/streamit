package at.dms.kjc.sir;

import at.dms.kjc.*;

/**
 * This class enumerates the types of splitters.
 */
public class SIRSplitType {
    /**
     * A duplicating splitter.
     */
    public static final SIRSplitType DUPLICATE = new SIRSplitType();
    /**
     * An equal-weight round robing splitter.
     */
    public static final SIRSplitType ROUND_ROBIN = new SIRSplitType();
    /**
     * A round robin splitter with individual weights for each tape.
     */
    public static final SIRSplitType WEIGHTED_RR = new SIRSplitType();
    /**
     * A null splitter, providing no tokens on its output.
     */
    public static final SIRSplitType NULL = new SIRSplitType();
    /**
     * Constructs a split type.
     */
    private SIRSplitType() {}
}
