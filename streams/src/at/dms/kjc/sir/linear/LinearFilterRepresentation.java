package at.dms.kjc.sir.linear;

import java.util.*;

/**
 * A LinearFilterRepresentation represents the computations performed by a filter
 * on its input values as a matrix and a vector. The matrix represents
 * the combinations of inputs used to create various outputs. The vector corresponds
 * to constants that are added to the combination of inputs to produce the outputs.<p>
 *
 * This class holds the A and b in the equation y = Ax+b which calculates the output
 * vector y using the input vector x. A is a matrix, b is a vector.<p>
 *
 * While this is not the clearest of descriptions, as this class is fleshed out
 * I hope to make the description more concise.<p>
 *
 * $Id: LinearFilterRepresentation.java,v 1.7 2002-09-18 01:02:50 aalamb Exp $
 **/
public class LinearFilterRepresentation {
    /** the A in y=Ax+b. **/
    private FilterMatrix A;
    /** the b in y=Ax+b. **/
    private FilterVector b;

    /**
     * The pop count of the filter. This is necessary for doing pipeline combinations
     * and it is information not stored somewhere in the dimensions of the
     * representation matrix or vector.
     **/
    private int popCount;

    /**
     * Create a new linear filter representation with matrix A and vector b.
     * Note that we use a copy of both matrix A and vector b so that we don't end up with
     * an aliasing problem. pc is the pop count of the filter that this represenation is for,
     * which we need for combining filters together (because the difference between
     * the peek count and the pop count tells us about the buffers that the user is using.
     **/
    public LinearFilterRepresentation(FilterMatrix matrixA,
				      FilterVector vectorb,
				      int pc) {
	this.A = matrixA.copy();
	this.b = (FilterVector)vectorb.copy();
	this.popCount = pc;
    }

    /** Get the A matrix. **/
    public FilterMatrix getA() {return this.A;}
    /** Get the b vector. **/
    public FilterVector getb() {return this.b;}

    /** Get the peek count. (#rows of A) **/
    public int getPeekCount() {return this.A.getRows();}
    /** Get the push count. (#cols of A) **/
    public int getPushCount() {return this.A.getCols();}
    /** Get the pop count. **/
    public int getPopCount() {return this.popCount;}

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
     * Expands the linear representation by the specified constant scaling factor.
     * See external documenation for details about how this works. I don't think that
     * the comments will be very happy, as is really easy to understand with a picture,
     * and very hard to understand without one.
     **/
    public LinearFilterRepresentation expand(int factor) {
	if (factor < 1) {
	    throw new IllegalArgumentException("can't expand linear filter representation by factor=" + factor + "<1");
	}
	int oldPush = this.getPushCount();
	int oldPeek = this.getPeekCount();
	int oldPop  = this.getPopCount();
	int newPush = oldPush * factor;
	int newPeek = oldPeek + (factor -1)*oldPop;
	int newPop  = oldPop * factor;
	FilterMatrix oldMatrix = this.getA();
	FilterMatrix newMatrix = new FilterMatrix(newPeek, newPush);

	// now, populate the new matrix with the appropriate copies of the old matrix
	// (eg the As).
	// copy over the first old matrix into the upper left hand corner
	newMatrix = newMatrix.copyAt(0,0,oldMatrix);
	for (int i=1; i<factor; i++) { // this many copies
	    // each one is offset by oldPop in the vertical direction, and offset by oldPush in
	    // the horizonytal direction.
	    newMatrix = newMatrix.copyAt(oldPeek+(i-1)*oldPop, i*oldPush, oldMatrix);
	}

	// also, duplicate the b vector factor times
	FilterVector oldVector = this.getb();
	FilterVector newVector = new FilterVector(newPush);
	for (int i=0; i<factor; i++) {
	    for (int j=0; j<oldPush; j++) {
		newVector.setElement((i*oldPush) +j,
				oldVector.getElement(j));
	    }
	}

	// create a new Linear rep for the expanded filter
	LinearFilterRepresentation newRep;
	newRep = new LinearFilterRepresentation(newMatrix, newVector, newPop);
	return newRep;
    }
						
	
	
}
