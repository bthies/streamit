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
    /**
     * Whether or not this is uniform--i.e., whether all the weights
     * in this are the same.  It might be the case that <uniform> is
     * false but all the weights are, in fact, the same (if formulas
     * for weighted rr's turn out to the the same); but if it's true,
     * it implies that they're definately the same.
     */
    private boolean uniform;

    private SIRJoiner(SIRContainer parent, 
		      SIRJoinType type, 
		      JExpression[] weights,
		      boolean uniform) {
      super(parent);
      this.weights = weights;
      this.type = type;
      this.uniform = uniform;
    }

    /**
     * Constructs a joiner with given parent, type and n number of inputs.
     */
    public static SIRJoiner create(SIRContainer parent,
				   SIRJoinType type, 
				   int n) {
	if (type==SIRJoinType.COMBINE) {
	    // fill weights with 1
	    return new SIRJoiner(parent, type, initLiteralArray(n, 1), true);
        } else if (type==SIRJoinType.NULL) {
	    // for null type, fill with zero weights
	    return new SIRJoiner(parent, type, initLiteralArray(n, 0), true);
	} else if (type==SIRJoinType.WEIGHTED_RR) {
	    // if making a weighted round robin, should use other constructor
	    fail("Need to specify weights for weighted round robin");
	} else if (type==SIRJoinType.ROUND_ROBIN) {
	    // if making a round robin, should use other constructor
	    fail("Need to specify weight for uniform round robin");
	} else {
	    fail("Unreckognized joiner type.");
	}
	// stupid compiler
	return null;
    }

    /**
     * Constructs a weighted round-robin joiner with the given parent
     * and weights.  Here the number of weights must match the number
     * of weights in the splitjoin.
     */
    public static SIRJoiner createWeightedRR(SIRContainer parent, 
					     JExpression[] weights) {
	return new SIRJoiner(parent, SIRJoinType.WEIGHTED_RR, weights, false);
    }

    /**
     * This is for creating a round robin with uniform weights across
     * the stream.  If weight[] is empty, then assume the weights
     * should all be one.  If non-empty, then assume the weight is the
     * first element of <weight>.
     */
    public static SIRJoiner createUniformRR(SIRContainer parent, 
					    JExpression[] weight) {
	JExpression weightExp;
	// get the weight for this
	if (weight.length==0) {
	    weightExp = new JIntLiteral(1);
	} else {
	    weightExp = weight[0];
	}
	// make a uniform rr joiner
	return new SIRJoiner(parent, 
			     SIRJoinType.WEIGHTED_RR,
			     initArray(1, weightExp),
			     true);
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
     * Accepts attribute visitor <v> at this node.
     */
    public Object accept(AttributeStreamVisitor v) {
	return v.visitJoiner(this,
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
	if (uniform) {
	    this.weights = initArray(extent, weights[0]);
	}
    }

    /**
     * Return the number of inputs to this.
     */
    public int getWays() {
	return weights.length;
    }

    /**
     * See doc in SIROperator.
     */
    public String getIdent() {
	return type.toString() + "_Joiner";
    }

    /**
     * Returns JExpression weights of this.
     */
    public JExpression[] getInternalWeights() {
	return weights;
    }

    /**
     * Returns the sum of all weights in this.
     */
    public int getSumOfWeights() {
	int[] weights = getWeights();
	int sum = 0;
	for (int i=0; i<weights.length; i++) {
	    sum += weights[i];
	}
	return sum;
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

