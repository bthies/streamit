package at.dms.kjc.sir.linear;

import java.util.*;

/**
 * A LinearFilterRepresentation represents the computations performed by a filter
 * on its input values as a matrix and a vector. The matrix represents
 * the combinations of inputs used to create various outputs. The vector corresponds
 * to constants that are added to the combination of inputs to produce the outputs.
 *
 * This class holds the A and b in the equation y = Ax+b which calculates the output
 * vector y using the input vector x. A is a matrix, b is a vector.
 *
 * While this is not the clearest of descriptions, as this class is fleshed out
 * I hope to make the description more concise.
 *
 * $Id: LinearFilterRepresentation.java,v 1.5 2002-09-16 19:02:32 aalamb Exp $
 **/
public class LinearFilterRepresentation {
    /** the A in y=Ax+b. **/
    private FilterMatrix A;
    /** the b in y=Ax+b. **/
    private FilterVector b;

    /**
     * Create a new linear filter representation with matrix A and vector b.
     * Note that we use a copy of both matrix A and vector b so that we don't end up with
     * an aliasing problem.
     **/
    public LinearFilterRepresentation(FilterMatrix matrixA,
				      FilterVector vectorb) {
	this.A = matrixA.copy();
	this.b = (FilterVector)vectorb.copy();
    }

    /** Get the A matrix. **/
    public FilterMatrix getA() {return this.A;}
    /** Get the b vector. **/
    public FilterVector getb() {return this.b;}

    /** Get the peek count. (#rows of A) **/
    public int getPeekCount() {return this.A.getRows();}
    /** Get the push count. (#cols of A) **/
    public int getPushCount() {return this.A.getCols();}

    /**
     * Returns true if at least one element of the constant vector b are zero.
     **/
    public boolean hasConstantComponent() {
	// go over all elements in b and if one is non zero, return true
	// otherwise return false.
	int bSize = b.getSize();
	for (int i=0; i<bSize; i++) {
	    ComplexNumber currentElement = b.getElement(i);
	    if (!currentElement.equals(ComplexNumber.ZERO)) {
		return true;
	    }
	}
	// seen only non-zero terms, therefore we don't have a
	// constant component.
	return false;
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
    LinearFilterRepresentation combine(LinearFilterRepresentation other) {
	// so we we can treat the (possibly) expanded versions the same 
	LinearFilterRepresentation thisRep  = this;
	LinearFilterRepresentation otherRep = other;

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
	    // we couldn't combine the matricies, give up
	    return null;
	}
    }
    
}
