package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.lowering.LoweringConstants;

/**
 * This represents a stream construct with a single output and multiple
 * inputs.
 */
public class SIRJoiner extends SIROperator {
    /**
     * This is a dummy work function that is used to represent the
     * work of a joiner to the scheduling package.  In the future this
     * could be replaced with a custom, instance-wise work function.
     */
    public static final JMethodDeclaration WORK_FUNCTION = 
	new JMethodDeclaration(/* tokref     */ null,
			       /* modifiers  */ at.dms.kjc.Constants.ACC_PUBLIC,
			       /* returntype */ CStdType.Void,
			       /* identifier -- important for uniprocessor */ LoweringConstants.JOINER_WORK_NAME,
			       /* parameters */ JFormalParameter.EMPTY, 
			       /* exceptions */ CClassType.EMPTY,
			       /* body       */ new JBlock(),
			       /* javadoc    */ null,
			       /* comments   */ null);
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

    public int oldSumWeights;

    /**
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    private SIRJoiner() {
	super();
    }
    
    private SIRJoiner(SIRContainer parent, 
		      SIRJoinType type, 
		      JExpression[] weights,
		      boolean uniform) {
      super(parent);
      this.weights = weights;
      this.type = type;
      this.uniform = uniform;
    }

    public static SIRJoiner create(SIRContainer parent, 
				   SIRJoinType type, 
				   JExpression[] weights) {
	if (type==SIRJoinType.NULL) {
	    return new SIRJoiner(parent, type, Utils.initLiteralArray(weights.length, 0), true);
	} else if (type==SIRJoinType.WEIGHTED_RR) {
	    return createWeightedRR(parent,weights);
	} else if (type==SIRJoinType.ROUND_ROBIN) {
	    JExpression weight = (weights.length>0 ? weights[0] : new JIntLiteral(1));
	    createUniformRR(parent,weight);
	} else {
	    Utils.fail("Unrecognized joiner type.");
	}
	// stupid compiler
	return null;
    }

    /**
     * Constructs a joiner with given parent, type and n number of inputs.
     */
    public static SIRJoiner create(SIRContainer parent,
				   SIRJoinType type, 
				   int n) {
	if (type==SIRJoinType.COMBINE) {
	    // fill weights with 1
	    return new SIRJoiner(parent, type, Utils.initLiteralArray(n, 1), true);
        } else if (type==SIRJoinType.NULL) {
	    // for null type, fill with zero weights
	    return new SIRJoiner(parent, type, Utils.initLiteralArray(n, 0), true);
	} else if (type==SIRJoinType.WEIGHTED_RR) {
	    // if making a weighted round robin, should use other constructor
	    Utils.fail("Need to specify weights for weighted round robin");
	} else if (type==SIRJoinType.ROUND_ROBIN) {
	    // if making a round robin, should use other constructor
	    Utils.fail("Need to specify weight for uniform round robin");
	} else {
	    Utils.fail("Unreckognized joiner type.");
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
     * the stream.
     */
    public static SIRJoiner createUniformRR(SIRContainer parent, 
					    JExpression weight) {
	// make a uniform rr joiner
	return new SIRJoiner(parent, 
			     SIRJoinType.WEIGHTED_RR,
			     Utils.initArray(Math.max(parent.size(), 1), weight),
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
	    return Utils.equalArrays(getWeights(), obj.getWeights());
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
	    this.weights = Utils.initArray(extent, weights[0]);
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

    public String toString() {
	return "SIRJoiner:"+getName();
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
	    result[i] = getWeight(i);
	}
	return result;
    }

    /**
     * Return int weight at position i.
     */
    public int getWeight(int i) {
	Utils.assert(weights[i] instanceof JIntLiteral,
		     "Expecting JIntLiteral as weight to round-robin--" +
		     "could have problems with constant prop (maybe " +
		     "it hasn't been run yet) or orig program");
	return ((JIntLiteral)weights[i]).intValue();
    }
}

