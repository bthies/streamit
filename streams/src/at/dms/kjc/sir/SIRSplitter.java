package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.*;

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
    private JExpression[] weights;
    /**
     * Whether or not this is uniform--i.e., whether all the weights
     * in this are the same.  It might be the case that <uniform> is
     * false but all the weights are, in fact, the same (if formulas
     * for weighted rr's turn out to the the same); but if it's true,
     * it implies that they're definately the same.
     */
    private boolean uniform;

    private SIRSplitter() {
	new RuntimeException("calling no-arg constructor").printStackTrace();
    }

    private SIRSplitter(SIRContainer parent, 
			SIRSplitType type, 
			JExpression[] weights,
			boolean uniform) {
      super(parent);
      this.weights = weights;
      this.type = type;
      this.uniform = uniform;
    }

    /**
     * Constructs a splitter with given parent, type and n number of inputs.
     */
    public static SIRSplitter create(SIRContainer parent, 
				     SIRSplitType type, 
				     int n) {
	if (type==SIRSplitType.DUPLICATE) {
	    // fill weights with 1
	    return new SIRSplitter(parent, type, initLiteralArray(n, 1), true);
        } else if (type==SIRSplitType.NULL) {
	    // for null type, fill with zero weights
	    return new SIRSplitter(parent, type, initLiteralArray(n, 0), true);
	} else if (type==SIRSplitType.WEIGHTED_RR) {
	    // if making a weighted round robin, should use other constructor
	    fail("Need to specify weights for weighted round robin");
	} else if (type==SIRSplitType.ROUND_ROBIN) {
	    // if making a round robin, should use other constructor
	    fail("Need to specify weight for uniform round robin");
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
    public static SIRSplitter createWeightedRR(SIRContainer parent, 
					       JExpression[] weights) {
	return new SIRSplitter(parent, SIRSplitType.WEIGHTED_RR, weights, 
			       false);
    }

    /**
     * This is for creating a round robin with uniform weights across
     * the stream.  If weight[] is empty, then assume the weights
     * should all be one.  If non-empty, then assume the weight is the
     * first element of <weight>.
     */
    public static SIRSplitter createUniformRR(SIRContainer parent, 
					      JExpression[] weight) {
	JExpression weightExp;
	// get the weight for this
	if (weight.length==0) {
	    weightExp = new JIntLiteral(1);
	} else {
	    weightExp = weight[0];
	}
	// make a uniform rr joiner
	return new SIRSplitter(parent, 
			       SIRSplitType.WEIGHTED_RR,
			       initArray(1, weightExp),
			       true);
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
	    return equalArrays(getWeights(), obj.getWeights());
	}
    }

    /**
     * If this is a splitter that has equal weight per way, then
     * rescale the weights to be of the given <extent>
     */
    public void rescale(int extent) {
	if (uniform) {
	    this.weights = initArray(extent, weights[0]);
	}
    }
    
    /**
     * Accepts visitor <v> at this node.
     */
    public void accept(StreamVisitor v) {
	v.visitSplitter(this,
			parent,
			type,
			weights);
    }

    /**
     * Accepts attribute visitor <v> at this node.
     */
    public Object accept(AttributeStreamVisitor v) {
	return v.visitSplitter(this,
			       parent,
			       type,
			       weights);
    }

    /**
     * Return type of this.
     */
    public SIRSplitType getType() {
	return type;
    }

    /**
     * Return the number of outputs of this.
     */
    public int getWays() {
	return weights.length;
    }

    /**
     * See doc in SIROperator.
     */
    public String getIdent() {
	return type.toString() + " Splitter";
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
			 "Expecting JIntLiteral as weight to round-robin--\n" +
			 "could have problems with constant prop (maybe \n" +
			 "it hasn't been run yet) or orig program.  This is\n" +
			 "in " + getParent());
	    result[i] = ((JIntLiteral)weights[i]).intValue();
	}
	return result;
    }
}
