package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.lowering.LoweringConstants;

/**
 * This represents a stream construct with a single input and multiple
 * outputs.
 */
public class SIRSplitter extends SIROperator {
    /**
     * This is a dummy work function that is used to represent the
     * work of a splitter to the scheduling package.  In the future
     * this could be replaced with a custom, instance-wise work
     * function.
     */
    public static final JMethodDeclaration WORK_FUNCTION = 
	new JMethodDeclaration(/* tokref     */ null,
			       /* modifiers  */ at.dms.kjc.Constants.ACC_PUBLIC,
			       /* returntype */ CStdType.Void,
			       /* identifier -- important for uniprocessor */ LoweringConstants.SPLITTER_WORK_NAME,
			       /* parameters */ JFormalParameter.EMPTY,
			       /* exceptions */ CClassType.EMPTY,
			       /* body       */ new JBlock(),
			       /* javadoc    */ null,
			       /* comments   */ null);
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

    /**
     * No-argument constructor just for cloning.
     */
    private SIRSplitter() {
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
    
    public static SIRSplitter create(SIRContainer parent, 
				     SIRSplitType type, 
				     JExpression[] weights) {
	if (type==SIRSplitType.DUPLICATE) {
	    return new SIRSplitter(parent, type, Utils.initLiteralArray(weights.length, 1), true);
        } else if (type==SIRSplitType.NULL) {
	    return new SIRSplitter(parent, type, Utils.initLiteralArray(weights.length, 0), true);
	} else if (type==SIRSplitType.WEIGHTED_RR) {
	    return createWeightedRR(parent,weights);
	} else if (type==SIRSplitType.ROUND_ROBIN) {
	    JExpression weight = (weights.length>0 ? weights[0] : new JIntLiteral(1));
	    createUniformRR(parent, weight);
	} else {
	    Utils.fail("Unreckognized splitter type.");
	}
	// stupid compiler
	return null;
    }


    /**
     * Constructs a splitter with given parent, type and n number of inputs.
     */
    public static SIRSplitter create(SIRContainer parent, 
				     SIRSplitType type, 
				     int n) {
	if (type==SIRSplitType.DUPLICATE) {
	    // fill weights with 1
	    return new SIRSplitter(parent, type, Utils.initLiteralArray(n, 1), true);
        } else if (type==SIRSplitType.NULL) {
	    // for null type, fill with zero weights
	    return new SIRSplitter(parent, type, Utils.initLiteralArray(n, 0), true);
	} else if (type==SIRSplitType.WEIGHTED_RR) {
	    // if making a weighted round robin, should use other constructor
	    Utils.fail("Need to specify weights for weighted round robin");
	} else if (type==SIRSplitType.ROUND_ROBIN) {
	    // if making a round robin, should use other constructor
	    Utils.fail("Need to specify weight for uniform round robin");
	} else {
	    Utils.fail("Unreckognized splitter type.");
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
     * the stream.
     */
    public static SIRSplitter createUniformRR(SIRContainer parent, 
					      JExpression weight) {
	return new SIRSplitter(parent, 
			       SIRSplitType.WEIGHTED_RR,
			       Utils.initArray(Math.max(parent.size(), 1), weight),
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
	    return Utils.equalArrays(getWeights(), obj.getWeights());
	}
    }

    /**
     * If this is a splitter that has equal weight per way, then
     * rescale the weights to be of the given <extent>
     */
    public void rescale(int extent) {
	if (uniform) {
	    this.weights = Utils.initArray(extent, weights[0]);
	}
    }
    
    /**
     * Accepts attribute visitor <v> at this node.
     */
    public Object accept(AttributeStreamVisitor v) {
	return v.visitSplitter(this,
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
	return type.toString() + "_Splitter";
    }

    public String toString() {
	return "SIRSplitter:"+getName();
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

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.sir.SIRSplitter other = new at.dms.kjc.sir.SIRSplitter();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRSplitter other) {
  super.deepCloneInto(other);
  other.type = (at.dms.kjc.sir.SIRSplitType)at.dms.kjc.AutoCloner.cloneToplevel(this.type, other);
  other.weights = (at.dms.kjc.JExpression[])at.dms.kjc.AutoCloner.cloneToplevel(this.weights, other);
  other.uniform = this.uniform;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
