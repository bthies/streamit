package at.dms.kjc.sir.statespace;

/**
 * A LinearFilterRepresentation represents the computations performed by a filter
 * on its input values as four matrices. 
 *
 * This class holds the A, B, C, D in the equations y = Ax+Bu, x' = Cx + Du which calculates the output
 * vector y and new state vector x' using the input vector u and the old state vector x.<br>
 *
 * $Id: LinearFilterRepresentation.java,v 1.2 2004-02-12 22:32:57 sitij Exp $
 * Modified to state space form by Sitij Agrawal  2/9/04
 **/


public class LinearFilterRepresentation {
    /** the A in y=Ax+Bu. **/
    private FilterMatrix A;
    /** the B in y=Ax+Bu. **/
    private FilterMatrix B;
    /** the C in x'=Cx+Du. **/
    private FilterMatrix C;
    /** the D in x'=Cx+Du. **/
    private FilterMatrix D;
    /** the cost of this node */
    private LinearCost cost;

    /**
     * The peek count of the filter. This is necessary for doing pipeline combinations
     * and it is information not stored in the dimensions of the
     * representation matrix or vector.
     **/
    private int peekCount;

    /**
     * Create a new linear filter representation with matrices A, B, C, and D.
     * Note that we use a copy of all matrices so that we don't end up with
     * an aliasing problem. peekc is the peek count of the filter that this represenation is for,
     * which we need for combining filters together (because the difference between
     * the peek count and the pop count tells us about the buffers that the program is using.
     **/
    public LinearFilterRepresentation(FilterMatrix matrixA,
				      FilterMatrix matrixB,
				      FilterMatrix matrixC,
				      FilterMatrix matrixD,
				      int peekc) {
	this.A = matrixA.copy();
	this.B = matrixB.copy();
	this.C = matrixC.copy();
	this.D = matrixD.copy();	
	this.peekCount = peekc;
	// we calculate cost on demain (with the linear partitioner)
	this.cost = null;
    }
    //////////////// Accessors ///////////////////
    
    /** Get the A matrix. **/
    public FilterMatrix getA() {return this.A;}
    /** Get the B matrix. **/
    public FilterMatrix getB() {return this.B;}
    /** Get the C matrix. **/
    public FilterMatrix getC() {return this.C;}
    /** Get the D matrix. **/
    public FilterMatrix getD() {return this.D;}

    /** Get the peek count.  **/
    public int getPeekCount() {return this.peekCount;}
    /** Get the push count. (#rows of D or C) **/
    public int getPushCount() {return this.D.getRows();}
    /** Get the pop count. (#cols of D) **/
    public int getPopCount() {return this.D.getCols();}


    //////////////// Utility Functions  ///////////////////

    /**
     * Returns true if at least one element of the constant vector b is zero.
     **/
    
    public boolean hasConstantComponent() {
	/*
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
	*/

	return false;

    }
    

    /**
     * Expands this linear representation to have the new peek, pop and push rates.
     * This method directly implements the "expand" operation outlined in
     * the "Linear Analysis and Optimization of Stream Programs" paper:
     * http://cag.lcs.mit.edu/commit/papers/03/pldi-linear.pdf
     **/
    /*
    public LinearFilterRepresentation expand(int newPeek, int newPop, int newPush) {
	// do some argument checks
	if (newPeek < this.getPeekCount()) {
	    throw new IllegalArgumentException("newPeek is less than old peek");
	}
	if (newPop < this.getPopCount()) {
	    throw new IllegalArgumentException("newPop is less than old push");
	}
	if (newPush < this.getPushCount()) {
	    throw new IllegalArgumentException("newPush is less than old push");
	}

	// pull out old values for ease in understanding the code.
	int oldPush = this.getPushCount();
	int oldPeek = this.getPeekCount();
	int oldPop  = this.getPopCount();
	FilterMatrix oldMatrix = this.getA();
	FilterMatrix newMatrix = new FilterMatrix(newPeek, newPush);

	// now, populate the new matrix with the appropriate copies of the old matrix
	// (eg the As). We will be copying numCompleteCopies starting from lower left
	int numCompleteCopies = (newPush/oldPush);
	for (int i=0; i<numCompleteCopies; i++) {
	    // copy the matrix starting at row: e' - e - (i*o)
	    // col = u'-(i+1)u
	    newMatrix.copyAt(newPeek - oldPeek - i*(oldPop),
			     newPush - (i+1)*oldPush,
			     oldMatrix);
	}

	// do housecleaning for any fractional copies of A that we need to make
	// (first, calculate the number of rows and columns that need to be filled with
	// parts of the old matrix).
	int numPartialRows = newPeek - numCompleteCopies*oldPop;
	int numPartialCols = newPush - numCompleteCopies*oldPush;

	// sanity checks.
	if (numPartialRows < 0) {throw new RuntimeException("partial rows < 0!  Partial rows = " + numPartialRows +
							    " newPeek=" + newPeek + " numCompleteCopies=" + numCompleteCopies + " oldPop=" + oldPop);}
	if (numPartialCols < 0) {throw new RuntimeException("partial cols < 0!");}

	// given the amount of debugging information below, you can tell
	// that this partitcular operation really sucked to implement -- lots
	// of silly details.
	
	//System.err.println("--------");
	//System.err.println("new rows: " + newPeek);
	//System.err.println("new cols: " + newPush);
	//System.err.println("new pop: "  + newPop);
	//System.err.println("old rows: " + oldPeek);
	//System.err.println("old cols: " + oldPush);
	//System.err.println("old pop: "  + oldPop);
	//System.err.println("num copies: " + numCompleteCopies);
	//System.err.println("partial rows: " + numPartialRows);
	//System.err.println("partial cols: " + numPartialCols);

	// now, copy over the missing parts of A
	for (int j=0; j<numPartialCols; j++) {
	    // now, we copy from top down
	    for (int i=0; i<numPartialRows;i++) {
		//System.err.println("i: " + i + ", j: " + j);
		int oldRow = oldPeek-(numPartialRows-i);
		int oldCol = oldPush-(numPartialCols-j);
		//System.err.println("oldRow: " + oldRow + " oldCol: " + oldCol);
		newMatrix.setElement(i,j,oldMatrix.getElement(oldRow, oldCol));
	    }
	}
	
	// now copy all elements of the new vector
	FilterVector oldVector = this.getb();
	FilterVector newVector = new FilterVector(newPush);
	for (int i=0; i<newPush; i++) {
	    newVector.setElement(i,oldVector.getElement(oldPush-1-((newPush-i-1)%oldPush)));
	}

	// create a new Linear rep for the expanded filter
	LinearFilterRepresentation newRep;
	newRep = new LinearFilterRepresentation(newMatrix, newVector, newPop);
	return newRep;
    }
    */	
					
    /**
     * Returns true if this filter is an FIR filter. A linear filter is FIR  
     * if push=pop=1 and no constant component.
     **/
    /*
    public boolean isFIR() {
	return ((this.getPopCount() == 1) &&
		(this.getPushCount() == 1) &&
		(this.getb().getElement(0,0).equals(ComplexNumber.ZERO)));
    }
    */

    /**
     * returns a LinearCost object that represents the number
     * of multiplies and adds that are necessary to implement this
     * linear filter representation.
     **/

    /*
    public LinearCost getCost() {
	if (this.cost==null) {
	    this.cost = calculateCost();
	}
	return this.cost;
    }
    */

    /**
     * Calculates cost of this.
     */
    /*
    private LinearCost calculateCost() {
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
	return new LinearCost(muls, adds, matRows, matCols, popCount);
    }	    
    */

    /** Returns true if and only if all coefficients in this filter rep are real valued. **/
    public boolean isPurelyReal() {
	// check the matrix(A), element by element.
	for (int i=0; i<A.getRows(); i++) {
	    for (int j=0; j<A.getCols(); j++) {
		if (!A.getElement(i,j).isReal()) {
		    return false;
		}
	    }
	}
	// check the matrix(B), element by element.
	for (int i=0; i<B.getRows(); i++) {
	    for (int j=0; j<B.getCols(); j++) {
		if (!B.getElement(i,j).isReal()) {
		    return false;
		}
	    }
	}
	// check the matrix(C), element by element.
	for (int i=0; i<C.getRows(); i++) {
	    for (int j=0; j<C.getCols(); j++) {
		if (!C.getElement(i,j).isReal()) {
		    return false;
		}
	    }
	}
	// check the matrix(D), element by element.
	for (int i=0; i<D.getRows(); i++) {
	    for (int j=0; j<D.getCols(); j++) {
		if (!D.getElement(i,j).isReal()) {
		    return false;
		}
	    }
	}	

	// if we get here, there are only real elemets in this rep
	return true;
    }
}



