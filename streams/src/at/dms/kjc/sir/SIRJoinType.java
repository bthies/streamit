package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.Utils;
import java.io.Serializable;

/**
 * This class enumerates the types of joiners.
 */
public class SIRJoinType implements Serializable, DeepCloneable {
    /**
     * A combining splitter.
     */
    public static final SIRJoinType COMBINE 
	= new SIRJoinType("COMBINE");
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
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    private SIRJoinType() {
	super();
    }
    
    /**
     * Constructs a join type with name <name>.
     */
    private SIRJoinType(String name) {
	this.name = name;
    }

    private Object readResolve() throws Exception {
	if (this.name.equals("COMBINE"))
	    return this.COMBINE;
	if (this.name.equals("ROUND_ROBIN"))
	    return this.ROUND_ROBIN;
	if (this.name.equals("WEIGHTED_ROUND_ROBIN"))
	    return this.WEIGHTED_RR;
	if (this.name.equals("NULL_SJ"))
	    return this.NULL;
	else 
	    throw new Exception();
    }

    public boolean isRoundRobin() {
	return name.equals("ROUND_ROBIN")||name.equals("WEIGHTED_ROUND_ROBIN");
    }
    
    public boolean isNull() {
	return name.equals("NULL_SJ");
    }

    public String toString() {
	return name;
    }


/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.sir.SIRJoinType other = new at.dms.kjc.sir.SIRJoinType();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRJoinType other) {
  other.name = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.name);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
