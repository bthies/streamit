package at.dms.kjc.sir;

import at.dms.kjc.*;

/**
 * This represents a stream construct with a single input and multiple
 * outputs.
 */
public class SIRSplitter extends SIROperator {
    /** 
     * The type of this joiner.
     */
    private SIRSplitType type;
    /** 
     * The number of items that the splitter pushes to each output tape
     * in one execution cycle.
     */
    private int[] weights;

    private SIRSplitter(SIRStream parent, SIRSplitType type, int[] weights) {
      super(parent);
      this.weights = weights;
      this.type = type;
    }

    /**
     * Constructs a splitter with given parent, type and n number of inputs.
     */
    public static SIRSplitter create(SIRStream parent, 
				     SIRSplitType type, 
				     int n) {
	if (type==SIRSplitType.ROUND_ROBIN || type==SIRSplitType.DUPLICATE) {
	    // fill weights with 1
	    return new SIRSplitter(parent, type, initArray(n, 1));
        } else if (type==SIRSplitType.NULL) {
	    // for null type, fill with zero weights
	    return new SIRSplitter(parent, type, initArray(n, 0));
	} else if (type==SIRSplitType.WEIGHTED_RR) {
	    // if making a weighted round robin, should use other constructor
	    fail("Need to specify weights for weighted round robin");
	} else {
	    fail("Unreckognized splitter type.");
	}
	// stupid compiler
	return null;
    }

    /**
     * Constructs a weighted round-robin splitter with the given
     * parent and weights.  
     */
    public static SIRSplitter createWeightedRR(SIRStream parent, 
					       int[] weights) {
	return new SIRSplitter(parent, SIRSplitType.WEIGHTED_RR, weights);
    }

    /**
     * Tests whether or not this has the same type and the same
     * weights as obj.  This can return true for splitters with
     * different numbers of outputs if the type is not a weighted
     * round robin.
     */
    public boolean equals(SIRSplitter obj) {
	if (obj.type!=SIRSplitType.WEIGHTED_RR || 
	        type!=SIRSplitType.WEIGHTED_RR) {
	    return type==obj.type;
	} else {
	    return equalArrays(weights, obj.weights);
	}
    }

    /**
     * Accepts visitor <v> at this node.
     */
    public void accept(SIRVisitor v) {
	v.visitSplitter(this,
			parent,
			type,
			weights);
    }
}
