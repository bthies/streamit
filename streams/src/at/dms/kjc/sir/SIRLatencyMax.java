package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.*;

/** 
 * This represents a maximum latency for message delivery.
 */
public class SIRLatencyMax extends SIRLatency implements Comparable {
    /**
     * The maximum latency.
     */
    protected JExpression max;

    /**
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    protected SIRLatencyMax() {
	super();
    }
    
    /**
     * Constructs a new latency with the given maximum.
     */
    public SIRLatencyMax(JExpression max) {
	this.max = max;
    }

    /**
     * Returns the maximum of this latency.
     */
    public JExpression getMaxExpression() {
	return max;
    }

    /**
     * Sets maximum of this latency.
     */
    public void setMaxExpression(JExpression _max) {
	max = _max;
    }

    /**
     * Returns the maximum of this latency.
     */
    public int getMax() {
	Utils.assert(max instanceof JIntLiteral, "Haven't resolved the max latency expression to a constant.  It is: " + max);
	return ((JIntLiteral)max).intValue();
    }

    /**
     * Compares the max time of this to that of <x>, returning the
     * difference.
     */
    public int compareTo(Object o) {
	return getMax() - ((SIRLatencyMax)o).getMax();
    }

    /**
     * Accepts the specified attribute visitor.
     * @param   p               the visitor
     */
    public Object accept(AttributeVisitor p) {
	if (p instanceof SLIRAttributeVisitor) {
	    return ((SLIRAttributeVisitor)p).visitLatencyMax(this);
	} else {
	    return this;
	}
    }

    /**
     * Accepts the specified visitor.
     */
    public void accept(KjcVisitor p) {
	if (p instanceof SLIRVisitor) {
	    ((SLIRVisitor)p).visitLatencyMax(this);
	} else {
	    at.dms.util.Utils.fail("Use SLIR visitor to visit an SIR node.");
	}
    }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.sir.SIRLatencyMax other = new at.dms.kjc.sir.SIRLatencyMax();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRLatencyMax other) {
  super.deepCloneInto(other);
  other.max = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.max, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

