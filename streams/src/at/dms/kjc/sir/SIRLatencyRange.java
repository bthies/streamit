package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.*;

/** 
 * This represents a range of latencies for message delivery.
 */
public class SIRLatencyRange extends SIRLatencyMax {
    /**
     * The minimum latency for this.  (The maximum latency is held in
     * the superclass.)
     */
    protected JExpression min;
    
    /**
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    protected SIRLatencyRange() {
	super();
    }
    
    /**
     * Constructs a latency with the range between <min> and <max>.
     */
    public SIRLatencyRange(JExpression min, JExpression max) {
	super(max);
	this.min = min;
    }

    /**
     * Returns the minimum of this latency.
     */
    public JExpression getMinExpression() {
	return min;
    }

    /**
     * Sets minimum of this latency.
     */
    public void setMinExpression(JExpression _min) {
	min = _min;
    }

    /**
     * Returns the minimum of this range.
     */
    public int getMin() {
	assert min instanceof JIntLiteral:
            "Haven't resolved the min latency expression to a constant.  " +
            "It is: " + min;
	return ((JIntLiteral)min).intValue();
    }

    /**
     * Returns whether or not this latency is constrained to be an
     * exact value, without a range of zero.  
     */
    public boolean isExact() {
	return min==max;
    }

    public String toString() {
	
	return "SIRLatencyRange min=" + min + " max=" + max;
    }

    /**
     * Accepts the specified attribute visitor.
     * @param   p               the visitor
     */
    public Object accept(AttributeVisitor p) {
	if (p instanceof SLIRAttributeVisitor) {
	    return ((SLIRAttributeVisitor)p).visitLatencyRange(this);
	} else {
	    return this;
	}
    }

    /**
     * Accepts the specified visitor.
     */
    public void accept(KjcVisitor p) {
	if (p instanceof SLIRVisitor) {
	    ((SLIRVisitor)p).visitLatencyRange(this);
	} else {
	    at.dms.util.Utils.fail("Use SLIR visitor to visit an SIR node.");
	}
    }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.sir.SIRLatencyRange other = new at.dms.kjc.sir.SIRLatencyRange();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRLatencyRange other) {
  super.deepCloneInto(other);
  other.min = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.min);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
