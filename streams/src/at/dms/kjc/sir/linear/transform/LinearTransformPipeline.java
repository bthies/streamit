package at.dms.kjc.sir.linear;

/**
 * Represents a pipeline combination transform. Combines two filter that
 * come one after another in a pipeline into a single filter that does
 * the same work. This combination might require each of the individual
 * filters to be expanded by some factor, and then a matrix multiplication
 * can be performed.
 **/
class LinearTransformPipeline extends LinearTransform {
    /** The upstream filter representation. **/
    LinearFilterRepresentation upstreamRep;
    /** the downstream filter representation. **/
    LinearFilterRepresentation downstreamRep;
    /** The number of times that we need to expand the upstream rep. **/
    int upstreamExpandFactor;
    /** The number of times that we need to expand the downstream rep. **/
    int downstreamExpandFactor;

    /**
     * Creates a new pipeline transformation by expanding the upstream rep
     * by a factor upFactor, expanding the downstream factor by downfactor
     * and then combining the two using the matrix multiply (see transform below).
     **/
    private LinearTransformPipeline(int upFactor,
				    LinearFilterRepresentation up,
				    int downFactor,
				    LinearFilterRepresentation down) {
	this.upstreamExpandFactor = upFactor;
	this.upstreamRep = up;
	this.downstreamExpandFactor = downFactor;
	this.downstreamRep = down;
    }


    /**
     * Tries to combine this LinearFilterRepresentation with other.
     * LFRs represent the calculations that a filter performs, by combining two
     * LFRs we hope to represent the calculations that the two filters cascaded one
     * after another form. <p>
     * 
     * Combining only makes sense for two filters with the following properties:
     * <ul>
     * <li> The push rate of upstream one is equal to the peek rate of the downstream one
     * </ul>
     *
     * It is interesting to note that the above suggests that some filters are not combinable
     * which we think is the general case. However, we can also possibly do the
     * equivalent of matrix unrolling on both this LFR and the other LFR to get the above
     * condition to hold.<p>
     *
     * If filter one computes y = xA1 + b1 and filter 2 computes y=xA2 + b2 then
     * the overall filter filter1 --> filter 2 will compute
     * y = (xA1 + b1)A2 + b2 = xA1A2 + (b1A2 + b2), which itself can be represented  
     * with the LFR: A = A1A2 and b = (b1A2 + b2).
     *
     * The LFR that represents both filters cascaded is returned if we can find it, and
     * null is returned if we can not.
     **/
    public LinearFilterRepresentation transform() throws NoTransformPossibleException {
	// so we we can refer to the (possibly) expanded versions in the same way  
	LinearFilterRepresentation thisRep  = this.upstreamRep;
	LinearFilterRepresentation otherRep = this.downstreamRep;

	// try and expand if need be
	// TODO

	// If the dimensions match up, then perform the actual matrix
	// multiplication
	if (thisRep.getPushCount() == otherRep.getPeekCount()) {
	    FilterMatrix A1 = thisRep.getA();
	    FilterVector b1 = thisRep.getb();
	    FilterMatrix A2 = otherRep.getA();
	    FilterVector b2 = otherRep.getb();
	    
	    // compute the the new A = A1A2
	    FilterMatrix newA = A1.times(A2);
	    // compute the new b = (b1A2 + b2)
	    FilterVector newb = FilterVector.toVector((b1.times(A2)).plus(b2));

	    // return a new LFR with newA and newb
	    return new LinearFilterRepresentation(newA, newb);
	} else {
	    // we couldn't combine the matricies, complain!
	    throw new RuntimeException("Pipeline is impossible to expand -- should not be a LinearTransformPipeline");
	}
    }

    /**
     * Calculates a LinearTransform defining how to transform a
     * pipeline combination of linear reps (eg one right after the next)
     * into a single linear rep. The filters look like
     * upstream->downstream, as you might imagine.
     **/
    public static LinearTransform calculate(LinearFilterRepresentation upstream,
					    LinearFilterRepresentation downstream) {
	if (upstream.getPushCount() == downstream.getPeekCount()) {
	    // we can multiply these pipelines together
	    return new LinearTransformPipeline(1, upstream,
					       1, downstream);
	    
	} else {
	    return new LinearTransformNull("Rates of upstream and downstream don't match.");
	}
    }


}
