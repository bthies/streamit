package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.Utils;

import streamit.scheduler.SchedJoinType;

/**
 * This class enumerates the types of joiners.
 */
public class SIRJoinType {
    /**
     * A duplicating splitter.
     */
    public static final SIRJoinType DUPLICATE 
	= new SIRJoinType("DUPLICATE");
    /**
     * An equal-weight round robing splitter.
     */
    public static final SIRJoinType ROUND_ROBIN 
	= new SIRJoinType("ROUND_ROBIN");
    /**
     * A round robin splitter with individual weights for each tape.
     */
    public static final SIRJoinType WEIGHTED_RR 
	= new SIRJoinType("WEIGHTED_ROUND_ROBIN");
    /**
     * A null splitter, providing no tokens on its output.
     */
    public static final SIRJoinType NULL 
	= new SIRJoinType("NULL_SJ");
    /**
     * The name of this type.
     */
    private String name;

    /**
     * Constructs a join type with name <name>.
     */
    private SIRJoinType(String name) {
	this.name = name;
    }

    public String toString() {
	return name;
    }

    /**
     * Returns an <int> that represents this type to the library/scheduler.
     */
    public int toSchedType() {
	if (this==ROUND_ROBIN) {
	    return SchedJoinType.ROUND_ROBIN;
	} else if (this==WEIGHTED_RR) {
	    return SchedJoinType.WEIGHTED_ROUND_ROBIN;
	} else {
	    Utils.fail("Type of joiner unsupported in library?");
	    return -1;
	}
    }
}
