package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.*;

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
    private JExpression[] weights;

    private SIRJoiner(SIRContainer parent, 
		      SIRJoinType type, 
		      JExpression[] weights) {
      super(parent);
      this.weights = weights;
      this.type = type;
    }

    /**
     * Constructs a joiner with given parent, type and n number of inputs.
     */
    public static SIRJoiner create(SIRContainer parent,
				   SIRJoinType type, 
				   int n) {
	if (type==SIRJoinType.ROUND_ROBIN || type==SIRJoinType.COMBINE) {
	    // fill weights with 1
	    return new SIRJoiner(parent, type, initLiteralArray(n, 1));
        } else if (type==SIRJoinType.NULL) {
	    // for null type, fill with zero weights
	    return new SIRJoiner(parent, type, initLiteralArray(n, 0));
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
    public static SIRJoiner createWeightedRR(SIRContainer parent, 
					     JExpression[] weights) {
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
	    return equalArrays(getWeights(), obj.getWeights());
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

    /**
     * Return type of this.
     */
    public SIRJoinType getType() {
	return type;
    }

    /**
     * If this is a joiner that has equal weight per way, then rescale
     * the weights to be of the given <extent>
     */
    public void rescale(int extent) {
	if (type==SIRJoinType.COMBINE ||
	    type==SIRJoinType.ROUND_ROBIN ||
	    type==SIRJoinType.NULL) {
	    this.weights = initLiteralArray(extent, ((JIntLiteral)
						     weights[0]).intValue());
	}
    }

    /**
     * Return the number of inputs to this.
     */
    public int getWays() {
	return weights.length;
    }

    /**
     * Returns JExpression weights of this.
     */
    public JExpression[] getInternalWeights() {
	return weights;
    }

    /**
     * Return int weights array of this.
     */
    public int[] getWeights() {
	int[] result = new int[weights.length];
	for (int i=0; i<weights.length; i++) {
	    Utils.assert(weights[i] instanceof JIntLiteral,
			 "Expecting JIntLiteral as weight to round-robin--" +
			 "could have problems with constant prop (maybe " +
			 "it hasn't been run yet) or orig program");
	    result[i] = ((JIntLiteral)weights[i]).intValue();
	}
	return result;
    }
}

