package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.Utils;

import streamit.scheduler.SchedSplitType;

/**
 * This class enumerates the types of splitters.
 */
public class SIRSplitType {
    /**
     * A duplicating splitter.
     */
    public static final SIRSplitType DUPLICATE 
	= new SIRSplitType("DUPLICATE");
    /**
     * An equal-weight round robing splitter.
     */
    public static final SIRSplitType ROUND_ROBIN 
	= new SIRSplitType("ROUND_ROBIN");
    /**
     * A round robin splitter with individual weights for each tape.
     */
    public static final SIRSplitType WEIGHTED_RR 
	= new SIRSplitType("WEIGHTED_ROUND_ROBIN");
    /**
     * A null splitter, providing no tokens on its output.
     */
    public static final SIRSplitType NULL 
	= new SIRSplitType("NULL_SJ");
    /**
     * The name of this type.
     */
    private String name;

    /**
     * Constructs a split type with name <name>.
     */
    private SIRSplitType(String name) {
	this.name = name;
    }

    /**
     * Returns an <int> that represents this type to the library/scheduler.
     */
    public int toSchedType() {
	if (this==ROUND_ROBIN) {
	    return SchedSplitType.ROUND_ROBIN;
	} else if (this==WEIGHTED_RR) {
	    return SchedSplitType.WEIGHTED_ROUND_ROBIN;
	} else if (this==DUPLICATE) {
	    return SchedSplitType.DUPLICATE;
	} else {
	    Utils.fail("Type of splitter unsupported in library?");
	    return -1;
	}
    }

    public String toString() {
	return name;
    }
}
