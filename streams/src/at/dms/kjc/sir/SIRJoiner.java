package at.dms.kjc.sir;

import at.dms.kjc.*;

/**
 * This represents a stream construct with a single output and multiple
 * inputs.
 */
public class SIRJoiner extends SIROperator {
    /** 
     * The type of this joiner.
     */
    private SIRJoinType type;
    /** 
     * The number of items that the joiner pulls from each input tape
     * in one execution cycle.
     */
    private int[] weights;

    private SIRJoiner(SIRStream parent, SIRJoinType type, int[] weights) {
      super(parent);
      this.weights = weights;
      this.type = type;
    }

    /**
     * Constructs a joiner with given parent, type and n number of inputs.
     */
    public static SIRJoiner create(SIRStream parent,
				   SIRJoinType type, 
				   int n) {
	if (type==SIRJoinType.ROUND_ROBIN || type==SIRJoinType.COMBINE) {
	    // fill weights with 1
	    return new SIRJoiner(parent, type, initArray(n, 1));
        } else if (type==SIRJoinType.NULL) {
	    // for null type, fill with zero weights
	    return new SIRJoiner(parent, type, initArray(n, 0));
	} else if (type==SIRJoinType.WEIGHTED_RR) {
	    // if making a weighted round robin, should use other constructor
	    fail("Need to specify weights for weighted round robin");
	} else {
	    fail("Unreckognized joiner type.");
	}
	// stupid compiler
	return null;
    }

    /**
     * Constructs a weighted round-robin joiner with the given parent
     * and weights.  
     */
    public static SIRJoiner createWeightedRR(SIRStream parent, int[] weights) {
	return new SIRJoiner(parent, SIRJoinType.WEIGHTED_RR, weights);
    }

    /**
     * Tests whether or not this has the same type and the same
     * weights as obj.  This can return true for splitters with
     * different numbers of outputs if the type is not a weighted
     * round robin.  */
    public boolean equals(SIRJoiner obj) {
	if (type    !=SIRJoinType.WEIGHTED_RR || 
	    obj.type!=SIRJoinType.WEIGHTED_RR) {
	    return type==obj.type;
	} else {
	    return equalArrays(weights, obj.weights);
	}
    }

    /**
     * Accepts visitor <v> at this node.
     */
    public void accept(StreamVisitor v) {
	v.visitJoiner(this,
		      parent,
		      type,
		      weights);
    }

    /**
     * Accepts attribute visitor <v> at this node.
     */
    public Object accept(AttributeStreamVisitor v) {
	return v.visitJoiner(this,
			     parent,
			     type,
			     weights);
    }
}

