package at.dms.kjc.sir.linear;

import java.util.*;

/**
 * A LinearFilterRepresentation represents the computations performed by a filter
 * on its input values as a matrix and a vector. The matrix represents
 * the combinations of inputs used to create various outputs. The vector corresponds
 * to constants that are added to the combination of inputs to produce the outputs.<p>
 *
 * This class holds the A and b in the equation y = Ax+b which calculates the output
 * vector y using the input vector x. A is a matrix, b is a (column) vector.<p>
 *
 * While this is not the clearest of descriptions, as this class is fleshed out
 * I hope to make the description more concise.<p>
 *
 * $Id: LinearFilterRepresentation.java,v 1.14 2002-11-25 20:31:58 aalamb Exp $
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
				      int popc) {
	this.A = matrixA.copy();
	this.b = (FilterVector)vectorb.copy();
	this.popCount = popc;
    }

    //////////////// Accessors ///////////////////
    
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


    //////////////// Utility Functions  ///////////////////

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

    /** placeholder to let us compile for regtests **/
    public LinearFilterRepresentation expand(int f) {
	throw new RuntimeException("deprecated");
    }

    /**
     * Expands this lienar representation to have the new peek, pop and push rates.
     * This method directly implements the "expand" operation outlined in
     * the "Linear Analysis and Optimization of Stream Programs" paper that we
     * submitted to pldi 03.
     **/
    public LinearFilterRepresentation expand(int newPeek, int newPop, int newPush) {
	// do some argument checks
	if (newPeek < this.getPeekCount()) {
	    throw new IllegalArgumentException("newPeek is less than old peek");
	}
	if (newPush < this.getPushCount()) {
	    throw new IllegalArgumentException("newPush is less than old push");
	}

	int factor = 1;
	
	int oldPush = this.getPushCount();
	int oldPeek = this.getPeekCount();
	int oldPop  = this.getPopCount();
	//int newPush = oldPush * factor;
	//int newPeek = oldPeek + (factor-1)*oldPop;
	//int newPop  = oldPop * factor;
	FilterMatrix oldMatrix = this.getA();
	FilterMatrix newMatrix = new FilterMatrix(newPeek, newPush);

	// now, populate the new matrix with the appropriate copies of the old matrix
	// (eg the As).
	// copy over the first old matrix into the upper left hand corner
	newMatrix.copyAt(0,0,oldMatrix);
	for (int i=1; i<factor; i++) { // this many copies
	    // each one is offset by oldPop in the vertical direction, and offset by oldPush in
	    // the horizonytal direction.
	    newMatrix.copyAt(i*oldPop, i*oldPush, oldMatrix);
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
						
    /**
     * Returns true if this filter is an FIR filter. A linear filter is FIR  
     * if push=pop=1 and no constant component.
     **/
    public boolean isFIR() {
	return ((this.getPopCount() == 1) &&
		(this.getPushCount() == 1) &&
		(this.getb().getElement(0,0).equals(ComplexNumber.ZERO)));
    }

    /**
     * returns a LinearCost object that represents the number
     * of multiplies and adds that are necessary to implement this
     * linear filter representation.
     **/
    public LinearCost getCost() {
	// add up multiplies and adds that are necessary for each column of the matrix. 
	int muls = 0;
	int adds = 0;

	int matRows = A.getRows();
	int matCols = A.getCols();
	
	for (int col=0; col<matCols; col++) {
	    // counters for the colums (# muls, adds)
	    int rowAdds = 0;
	    int rowMuls =  0;
	    for (int row=0; row<matRows; row++) {
		ComplexNumber currentElement = A.getElement(row,col);
		if (!currentElement.isReal()) {
		    throw new RuntimeException("Non real matrix elements are not supported in cost .");
		}
		// flags on whether or not to increment the counters
		boolean incAdd = true;
		boolean incMul = true;
		// if it is zero, no add or mult is necessary
		if (currentElement.equals(ComplexNumber.ZERO)) {
		    incAdd = false;
		    incMul = false;
		// if one, no need to do a multiplication.
		} else if (currentElement.equals(ComplexNumber.ONE)) {
		    incMul = false;
		}
		// now, increment if our increment flags are set.
		if (incAdd) {rowAdds++;}
		if (incMul) {rowMuls++;}
	    }
	    // now, add in the contribution from the constant vector
	    ComplexNumber currentElement = b.getElement(0,col);
	    if (!currentElement.isReal()) {
		throw new RuntimeException("Non real vector elements are not supported in cost .");
	    }

	    // nothing for zero, inc add for one, and inc both for anything else.
	    if (currentElement.equals(ComplexNumber.ZERO)) {
	    } else if (currentElement.equals(ComplexNumber.ONE)) {
		rowAdds++;
	    } else {
		rowAdds++;
		rowMuls++;
	    }

	    // basically, we need one less add per row because adds take two operands
	    // however, we don't want to blindly subtract one, because that might give
	    // us a negative number
	    if (rowAdds > 0) {rowAdds--;}
	    // stick row counters onto overall counters
	    muls += rowMuls;
	    adds += rowAdds;
	}
	return new LinearCost(muls, adds);
    }	    

}
