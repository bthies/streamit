package at.dms.kjc.sir.linear;

import java.util.*;

/**
 * A FilterMatrix contains a matrix representation of a
 * linear filter in StreamIt. A Linear filter is a filter
 * for which each item pushed is a linear combination
 * of the input. If you think about the items that are peeked as
 * an input vector, you can express the filter's operation as
 * a matrix multiply operation on the input vector which
 * produces an output vector.
 *
 * Each element of the FilterMatrix is a ComplexNumber
 *
 * $Id: FilterMatrix.java,v 1.18 2003-04-11 00:14:30 aalamb Exp $
 **/

public class FilterMatrix {
    /** Internal representation of the matrix **/ 
    private ComplexNumber internalMatrix[][] = null;
    private int internalSizeRows = -1;
    private int internalSizeCols = -1;
    
    /** empty constructor so that I can subclass with FilterMatrixReal. **/
    FilterMatrix() {
	super();
    }


    /**
     * Create FilterMatrix of size (rows,cols) and initialize all
     * elements to the value 0.
     **/
    public FilterMatrix(int rows, int cols) {
	// a little sanity check to make sure we aren't starting with negative dimensions
	if ((rows <= 0) || (cols <= 0)) {
	    throw new IllegalArgumentException("Illegal dimensions:("+rows+","+cols+")");
	}
	// instantiate the internal matrix, and save the dimensions
 	this.internalMatrix = new ComplexNumber[rows][cols];
	this.internalSizeRows = rows;
	this.internalSizeCols = cols;
	// initialize all to 0 
	for (int i=0; i<rows; i++) {
	    for (int j=0; j<cols; j++) {
		this.internalMatrix[i][j] = ComplexNumber.ZERO;
	    }
	}
	//checkRep();
    }


    /**
     * Accessor: returns the value of the matrix at the specified position,
     * and bombs an exception if the value is out of range.
     **/
    public ComplexNumber getElement(int row, int col) {
	//checkRep();
	// do bounds checking
	validateBounds(row,col);
	// actually give back the value
	return this.internalMatrix[row][col];
    }

    /**
     * Accessor: Return the number of rows in this matrix.
     **/
    public int getRows() {
	//checkRep();
	return this.internalSizeRows;
    }
    /**
     * Accessor: Return the number of columns in this matrix.
     **/
    public int getCols() {
	//checkRep();
	return this.internalSizeCols;
    }

    
    /**
     * Sets the element in (row,col) to be value.
     **/
    public void setElement(int row, int col, ComplexNumber value) {
	// check bounds
	validateBounds(row, col);
	// make sure that we aren't putting in null
	if (value == null) {throw new IllegalArgumentException("Null arguments are not allowed"); }
	// finally, set the value correctly
	this.internalMatrix[row][col] = value;
	// make sure that we haven't foobared the rep
	//checkRep();
    }

    /** convenience method -- automatically creates an entry with the specified real number **/
    public void setElement(int row, int col, double realNumber) {
	this.setElement(row,col,new ComplexNumber(realNumber,0));
    }
	

    /**
     * Ensure that the specified row and col are within the
     * internal matrix's size. Throw an exception if they are not.
     **/
    void validateBounds(int row, int col) {
	validateBounds(row, col, this.internalSizeRows, this.internalSizeCols);
    }
    /**
     * Function that does the actual work of validate bounds.
     **/
    void validateBounds(int row, int col, int numRows, int numCols) {
	if (row < 0) {
	    throw new IllegalArgumentException("Row " + row + " is less than one.");
	}
	if (col < 0) {
	    throw new IllegalArgumentException("Column " + col + " is less than one.");
	}
	if (row >= numRows) {
	    throw new IllegalArgumentException("Row " + row + " is greater than size:" +
					       "(" + numRows + "," +
					       numCols + ")");
	}
	if (col >= numCols) {
	    throw new IllegalArgumentException("Column " + col + " is greater than size:" +
					       "(" + numRows + "," +
					       numCols + ")");
	}

    }

    /** Return a copy of this FilterMatrix. **/
    public FilterMatrix copy() {
	//checkRep();
	FilterMatrix copyMatrix = new FilterMatrix(this.internalSizeRows,
						   this.internalSizeCols);

	// easy implementation -- just copy element by element
	for (int i=0; i<this.internalSizeRows; i++) {
	    for (int j=0; j<this.internalSizeCols; j++) {
		// this is ok because ComplexNumbers are immutable
		copyMatrix.setElement(i,j, this.getElement(i,j));
	    }
	}
	return copyMatrix;
    }

    /**
     * Copies the source matrix into this FilterMatrix such that the
     * specified ofset is the top left hand corner of the small matrix.
     * Throws (horrible) exceptions when the bounds of the smaller
     * matrix at the offset overrun the boundaries of this matrix.
     **/
    public void copyAt(int offsetRow, int offsetCol, FilterMatrix sourceMatrix) {
	if (sourceMatrix == null) {throw new IllegalArgumentException("Null source matrix");}
	// make sure that the copy won't run off the end of the this matrix
	int sourceRows = sourceMatrix.getRows();
	int sourceCols = sourceMatrix.getCols();
	if ((offsetRow + sourceRows) > this.internalSizeRows) {
	    throw new IllegalArgumentException("copying sourceMatrix would exceed matrix row size.");
	}
	if ((offsetCol + sourceCols) > this.internalSizeCols) {
	    throw new IllegalArgumentException("copying sourceMatrix would exceed matrix col size.");
	}

	// now that we are satisfied that the boundaries are ok, do the copy.
	for (int i=0; i<sourceRows; i++) {
	    for (int j=0; j<sourceCols; j++) {
		this.setElement(offsetRow + i, offsetCol + j,
				 sourceMatrix.getElement(i,j));
	    }
	}

	return;
    }

    /**
     * Copies numCols from the source matrix starting at (0,sourceOffset)
     * into this matrix beginning at (0,destOffset).
     * (This is used to merge columns from one filter matrix into another
     * during splitjoin combinations.)
     **/
    public void copyColumnsAt(int destOffset, FilterMatrix sourceMatrix, int srcOffset, int numCols) {
	String argString = ("destOffset: " + destOffset + ". srcOffset: " + srcOffset + ". numCols: " + numCols);
	if (sourceMatrix == null) {throw new IllegalArgumentException("Null source matrix");}
	// First, do some crazy bounds checking
	// make sure that the number of rows is equal
	if (this.internalSizeRows != sourceMatrix.internalSizeRows) {
	    //System.err.println("Args: " + argString);
	    //System.err.println("this: \n" + this);
	    //System.err.println("source:\n" + sourceMatrix);
	    throw new IllegalArgumentException("Source and destination marices don't have the same # of rows.");
	}
	    
	// make sure that the dest offset + numCols doesn't overrun the bounds of this
	if (this.internalSizeCols < (destOffset + numCols)) {
	    throw new IllegalArgumentException("Copy past the right side of this matrix:" + argString);
	}
	// make sure that the source offset + numCols doesn't overrun the bounds of source
	if (sourceMatrix.internalSizeCols < (srcOffset + numCols)) {
	    throw new IllegalArgumentException("Copy from past the right side of the source matrix");
	}
	    

	// for each row
	for (int i=0; i<this.internalSizeRows; i++) {
	    // copy each column
	    for (int j=0; j<numCols; j++) {
		// do the copy
		this.internalMatrix[i][destOffset+j] = sourceMatrix.internalMatrix[i][srcOffset+j];
	    }
	}
    }

    /**
     * Copied numRows at the thisOffset position of this filter matrix
     * from sourceOffset of the source filter matrix.
     **/
    public void copyRowsAt(int thisOffset, FilterMatrix source, int sourceOffset, int numRows) {
	if (source == null) {throw new IllegalArgumentException("null source matrix");}
	// check bounds
	String args = ("(thisOffset=" + thisOffset + ",sourceOffset=" +
		       sourceOffset + ",numRows=" + numRows + ")");
	if (this.internalSizeCols != source.internalSizeCols) {
	    throw new IllegalArgumentException("row sizes don't match up (eg # cols are different)");
	}
	if (this.internalSizeRows < (thisOffset + numRows)) {
	    throw new IllegalArgumentException("too many rows to copy -- run off end of dest." + args);
	}
	if (source.internalSizeRows < (sourceOffset + numRows)) {
	    throw new IllegalArgumentException("too many rows to copy -- run off the end of source");
	}
	// do the copy
	for (int i=0; i<numRows; i++) {
	    for (int j=0; j<this.internalSizeCols; j++) {
		this.internalMatrix[i+thisOffset][j] = source.internalMatrix[sourceOffset+i][j];
	    }
	}
    }
     

    /**
     * Returns the product of this matrix and the specified matrix.
     * Note that the dimensions must be compatible,
     * (#cols of this == #rows of other)
     * or else an IllegalArgumentException is thrown.
     * Returns Q = this*other. (order is important for matrix multiplys)
     **/
    public FilterMatrix times(FilterMatrix other) {
	if (other == null) {throw new IllegalArgumentException("Null other in times()");}
	// check the dimensions
	if (this.getCols() != other.getRows()) {
	    throw new IllegalArgumentException("Dimensions do not agree in matrix multiply");
	}
	// just implement matrix multiply straight up, one element after another
	FilterMatrix product = new FilterMatrix(this.getRows(), other.getCols());
	// for each row of this
	for (int i=0; i<this.getRows(); i++) {
	    // for each col of other
	    for (int j=0; j<other.getCols(); j++) {
		ComplexNumber sum = ComplexNumber.ZERO;
		// for each element, compute partial sum (eg this(i,k)other(k,j)
		for (int k=0; k<this.getCols(); k++) {
		    // sum = sum + (this(i,k)*other(k,j)
		    sum = sum.plus(this.getElement(i,k).times(other.getElement(k,j)));
		}
		// set the element i,j in the new matrix
		product.setElement(i,j,sum);
	    }
	}
	return product;
    }

    /**
     * Return the element-wise sum of this matrix with other.
     **/
    public FilterMatrix plus(FilterMatrix other) {
	if (other==null){throw new IllegalArgumentException("null other in plus()");}
	// check dimensions -- all must agree
	if ((this.getRows() != other.getRows()) ||
	    (this.getCols() != other.getCols())) {
	    throw new IllegalArgumentException("Dimension mismatch in FilterMatrix.plus!");
	}
	
	// now, make the sum and add the vectors element wise
	FilterMatrix sum = new FilterMatrix(this.getRows(), this.getCols());
	for (int i=0; i<this.getRows(); i++) {
	    for (int j=0; j<this.getCols(); j++) {
		ComplexNumber thisNum  = this.getElement(i,j);
		ComplexNumber otherNum = other.getElement(i,j);
		sum.setElement(i,j,thisNum.plus(otherNum));
	    }
	}
	return sum;
    }	

    /**
     * Return the transpose of this matrix.
     */
    public FilterMatrix transpose() {
	FilterMatrix result = new FilterMatrix(this.getCols(), this.getRows());
	for (int i=0; i<this.getRows(); i++) {
	    for (int j=0; j<this.getCols(); j++) {
		result.setElement(j, i, getElement(i, j));
	    }
	}
	return result;
    }

    /**
     * Return whether or not all the elements of this have integral
     * components.
     */
    public boolean isIntegral() {
	for (int i=0; i<getRows(); i++) {
	    for (int j=0; j<getCols(); j++) {
		if (!getElement(i, j).isIntegral()) {
		    return false;
		}
	    }
	}
	return true;
    }

    /**
     * Return whether or not all the elements of this are real.
     */
    public boolean isReal() {
	for (int i=0; i<getRows(); i++) {
	    for (int j=0; j<getCols(); j++) {
		if (!getElement(i, j).isReal()) {
		    return false;
		}
	    }
	}
	return true;
    }

    /**
     * Return true if the passed object is a FilterMatrix and represents the same matrix as this,
     * on an element by element basis.
     **/
    public boolean equals(Object o) {
	if (o == null) {return false;}
	if (!(o instanceof FilterMatrix)) {return false;}
	FilterMatrix other = (FilterMatrix)o;

	// compare sizes
	if (this.getRows() != other.getRows()) {return false;}
	if (this.getCols() != other.getCols()) {return false;}

	// now, compare element by element
	for (int i=0; i<this.internalSizeRows; i++) {
	    for (int j=0; j<this.internalSizeCols; j++) {
		// if the elements are not the same, we are done
		if (!this.getElement(i,j).equals(other.getElement(i,j))) {
		    return false;
		}
	    }
	}

	// if we get here, the other matrix is the same as this one.
	return true;
    }

    /**
     * Return true if the passed object is a FilterMatrix with the
     * same dimensions as this, that has zeros/ones (and all its
     * zeros and ones) at exactly the same locations as this.
     **/
    public boolean hasEqualZeroOneElements(FilterMatrix other) {
	// compare sizes
	if (this.getRows() != other.getRows()) {return false;}
	if (this.getCols() != other.getCols()) {return false;}

	// now, compare element by element
	for (int i=0; i<this.internalSizeRows; i++) {
	    for (int j=0; j<this.internalSizeCols; j++) {
		boolean thisZero = this.getElement(i,j).equals(ComplexNumber.ZERO);
		boolean otherZero = other.getElement(i,j).equals(ComplexNumber.ZERO);
		if (thisZero != otherZero) {
		    return false;
		}
		boolean thisOne = this.getElement(i,j).equals(ComplexNumber.ONE);
		boolean otherOne = other.getElement(i,j).equals(ComplexNumber.ONE);
		if (thisOne != otherOne) {
		    return false;
		}
	    }
	}
	// if we get here, the other matrix is the same as this one.
	return true;
    }

    /**
     * Preserve the semantics of equals/hashCode. If two objects
     * are equal, they should produce the same hash code.
     **/
    public int hashCode() {
	return 1;
    }

    /**
     * Scales all elements in this matrix by the specified constant.
     * Obviously, this method therefore mutates the matrix.
     **/
    public void scale(ComplexNumber factor) {
	if (factor == null) {
	    throw new IllegalArgumentException("null factor");
	}
	// for each element in the matrix, scale by the factor
	for (int i=0; i<this.internalSizeRows; i++) {
	    for (int j=0; j<this.internalSizeCols; j++) {
		ComplexNumber originalElement = this.getElement(i,j);
		// perform the multiplication
		ComplexNumber scaledElement = originalElement.times(factor);
		// update the matrix
		this.setElement(i,j,scaledElement);
	    }
	}
	// make sure that we haven't screwed things up.
	//checkRep();
    }
    

    /** check to make sure that our assumptions about internal state hold true **/
    private void checkRep() {
	if (this.internalMatrix == null) {
	    throw new RuntimeException("Null internal representation");
	}
	// make sure that the dimensions match up
	if (this.internalSizeRows != this.internalMatrix.length) {
	    throw new RuntimeException("Row size mismatch");
	}
	if (this.internalSizeCols != this.internalMatrix[0].length) {
	    throw new RuntimeException("Col size mismatch");
	}
	// make sure that all of the elements are not null
	for (int i=0; i<this.internalSizeRows; i++) {
	    for (int j=0; j<this.internalSizeCols; j++) {
		if (this.internalMatrix[i][j] == null) {
		    throw new RuntimeException("Null matrix entry");
		}
	    }
	}	
    }	


    /** Pretty Print our matrix **/
    public String toString() {
	//checkRep();
	String returnString = "[";
	// for each row
	for (int i=0; i<internalSizeRows; i++) {
	    if (i != 0) {
		returnString += " ";
	    }
	    returnString += "[";
	    // for each column
	    for (int j=0; j<internalSizeCols; j++) {
		// stick the value of the matrix onto this line
		returnString += this.internalMatrix[i][j];
		// don't add a space if this is the last element in the column
		if (j != (internalSizeCols-1)) {
		    returnString += " ";
		}
	    }
	    returnString += "]";
	    if (i != (internalSizeRows-1)) {
		returnString += "\n";
	    }
	}
	returnString += "]";

	return returnString;
    }

    /**
     * Print only a "0" for zero elements and an "x" for non-zero elements
     */
    public String getZeroString() {
	//checkRep();
	String returnString = "[";
	// for each row
	for (int i=0; i<internalSizeRows; i++) {
	    if (i != 0) {
		returnString += " ";
	    }
	    returnString += "[";
	    // for each column
	    for (int j=0; j<internalSizeCols; j++) {
		// stick the value of the matrix onto this line
		if (this.internalMatrix[i][j].equals(ComplexNumber.ZERO)) {
		    returnString += "0";
		} else {
		    returnString += "x";
		}
		// don't add a space if this is the last element in the column
		if (j != (internalSizeCols-1)) {
		    returnString += " ";
		}
	    }
	    returnString += "]";
	    if (i != (internalSizeRows-1)) {
		returnString += "\n";
	    }
	}
	returnString += "]";

	return returnString;
    }

}
