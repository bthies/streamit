package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.util.Utils;

/**
 * A FilterMatrix represents a matrix for use in the 
 * linear filter analysis of StreamIt. The strange copy methods
 * are used in the linear filter combination rules.<br>
 *
 * Each element of the FilterMatrix is a ComplexNumber,
 * though the internal representation was changed to
 * two associated arrays of floats for performance reasons.<br>
 *
 * Note: In the current implementation, if a matrix element is ever
 * assigned an imaginary value, the matrix is forever designated as
 * have imaginary parts, even if the imaginary element is reassigned
 * a real value. This isn't a problem currently because we don't actually
 * ever use imaginary values in the compiler, but if people
 * actually start using FilterMatrices for imaginary entries, then
 * someone should implement an imaginary entry counting scheme. -- AAL<br>
 *
 * $Id: FilterMatrix.java,v 1.22 2003-10-20 06:37:55 thies Exp $
 **/

public class FilterMatrix {
    /** Internal representation of the matrix (real). **/ 
    private double internalMatrixReal[][] = null;
    /** Internal representation of the matrix (imag). **/
    private double internalMatrixImag[][] = null;
    /** This flag is used to easily differentiate between real amd complex matrices. **/
    private boolean realFlag = true;
    private int internalSizeRows = -1;
    private int internalSizeCols = -1;
    
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
 	this.internalMatrixReal = new double[rows][cols];
	this.internalMatrixImag = new double[rows][cols];
	this.internalSizeRows = rows;
	this.internalSizeCols = cols;
    }


    /**
     * Accessor: returns the value of the matrix at the specified position,
     * and bombs an exception if the value is out of range.
     **/
    public ComplexNumber getElement(int row, int col) {
	// do bounds checking
	validateBounds(row,col);
	// actually give back the value
	return new ComplexNumber(this.internalMatrixReal[row][col],
				 this.internalMatrixImag[row][col]);
    }

    /**
     * Accessor: Return the number of rows in this matrix.
     **/
    public int getRows() {
	return this.internalSizeRows;
    }
    /**
     * Accessor: Return the number of columns in this matrix.
     **/
    public int getCols() {
	return this.internalSizeCols;
    }

    /**
     * Sets the element in (row,col) to be value.
     **/
    public void setElement(int row, int col, ComplexNumber value) {
	// check bounds
	validateBounds(row, col);
	// make sure that we aren't putting in null
	if (value == null) {
	    throw new IllegalArgumentException("Null arguments are not allowed");
	}
	// finally, set the value correctly
	this.internalMatrixReal[row][col] = value.getReal();
	// if the imaginary part is non-zero, this matrix is now non-real
	double imagValue = value.getImaginary();
	if (imagValue != 0) {
	    this.realFlag = false;
	    this.internalMatrixImag[row][col] = imagValue;
	}
    }

    /**
     * Convenience method that automatically sets an entry to
     * the specified real number.
     **/
    public void setElement(int row, int col, double realNumber) {
	validateBounds(row, col);
	this.internalMatrixReal[row][col] = realNumber;
    }

    /**
     * Returns a "trimmed" version of this, in which unused (0) peek
     * values are cut off.  Unused peek values are rows of zeros
     * at the top of the matrix.
     * However, guarantees to preserve at least
     * <minRows>, since some things seem to depend on the rows being
     * at least as big as the pop count.
     * This method is designed to reduce the peek value of linear
     * filters without changing the semantics.
     **/
    public FilterMatrix trim(int minRows) {
	Utils.assert(this.isReal());
	int zeroRows = 0;
	boolean allZero = true;
	while (allZero && (internalSizeRows-zeroRows>minRows)) {
	    allZero = true;
	    for (int j=0; j<internalSizeCols; j++) {
		if (internalMatrixReal[zeroRows][j]!=0) {
		    allZero = false;
		    break;
		}
	    }
	    if (allZero) { zeroRows++; }
	    // figure if we found an all-zero matrix, then there was a reason for it...
	}
	// return new matrix
	FilterMatrix result = new FilterMatrix(internalSizeRows-zeroRows, internalSizeCols);
	for (int i=zeroRows; i<internalSizeRows; i++) {
	    for (int j=0; j<internalSizeCols; j++) {
		result.setElement(i-zeroRows, j, internalMatrixReal[i][j]);
	    }
	}
	return result;
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
	FilterMatrix copyMatrix = new FilterMatrix(this.internalSizeRows,
						   this.internalSizeCols);

	// easy implementation -- just copy element by element (using information
	// about underlying impelementation
	for (int i=0; i<this.internalSizeRows; i++) {
	    for (int j=0; j<this.internalSizeCols; j++) {
		copyMatrix.internalMatrixReal[i][j] = this.internalMatrixReal[i][j];
	    }
	}
	// do the same copy if the real flag is false
	if (realFlag == false) {
	    for (int i=0; i<this.internalSizeRows; i++) {
		for (int j=0; j<this.internalSizeCols; j++) {
		    copyMatrix.internalMatrixImag[i][j] = this.internalMatrixImag[i][j];
		}
	    }
	}
	return copyMatrix;
    }

    /**
     * Copies the source matrix into this FilterMatrix such that the
     * specified offset is the top left hand corner of the small matrix.
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
		this.internalMatrixReal[offsetRow + i][offsetCol + j] =
		    sourceMatrix.internalMatrixReal[i][j];
	    }
	}
	// if we have any complex data, copy that too
	if (this.realFlag == false) {
	    for (int i=0; i<sourceRows; i++) {
		for (int j=0; j<sourceCols; j++) {
		    this.internalMatrixImag[offsetRow + i][offsetCol + j] =
			sourceMatrix.internalMatrixImag[i][j];
		}
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
    public void copyColumnsAt(int destOffset, FilterMatrix sourceMatrix,
			      int srcOffset, int numCols) {
	String argString = ("destOffset: " + destOffset +
			    ". srcOffset: " + srcOffset + ". numCols: " + numCols);
	if (sourceMatrix == null) {throw new IllegalArgumentException("Null source matrix");}
	// First, do some crazy bounds checking:
	// 1) make sure that the number of rows is equal
	if (this.internalSizeRows != sourceMatrix.internalSizeRows) {
	    //System.err.println("Args: " + argString);
	    //System.err.println("this: \n" + this);
	    //System.err.println("source:\n" + sourceMatrix);
	    throw new IllegalArgumentException("Source and dest marices don't have the same # of rows.");
	}
	// 2) make sure that the dest offset + numCols doesn't overrun the bounds of this
	if (this.internalSizeCols < (destOffset + numCols)) {
	    throw new IllegalArgumentException("Copy past the right side of this matrix:" + argString);
	}
	// 3) make sure that the source offset + numCols doesn't overrun the bounds of source
	if (sourceMatrix.internalSizeCols < (srcOffset + numCols)) {
	    throw new IllegalArgumentException("Copy from past the right side of the source matrix");
	}
	// finally, do the copy for the real data
	for (int i=0; i<this.internalSizeRows; i++) {
	    for (int j=0; j<numCols; j++) {
		this.internalMatrixReal[i][destOffset+j] = sourceMatrix.internalMatrixReal[i][srcOffset+j];
	    }
	}

	// if we have imaginary parts, copy them too
	if (this.realFlag == false) {
	    for (int i=0; i<this.internalSizeRows; i++) {
		for (int j=0; j<numCols; j++) {
		    this.internalMatrixImag[i][destOffset+j] = sourceMatrix.internalMatrixImag[i][srcOffset+j];
		}
	    }
	}

    }

    /**
     * Copied numRows at the thisOffset position of this FilterMatrix
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
	// do the copy for reals
	for (int i=0; i<numRows; i++) {
	    for (int j=0; j<this.internalSizeCols; j++) {
		this.internalMatrixReal[i+thisOffset][j] = source.internalMatrixReal[sourceOffset+i][j];
	    }
	}
	// if we have imaginary data
	if (this.realFlag == false) {
	    for (int i=0; i<numRows; i++) {
		for (int j=0; j<this.internalSizeCols; j++) {
		    this.internalMatrixImag[i+thisOffset][j] = source.internalMatrixImag[sourceOffset+i][j];
		}
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
	    throw new IllegalArgumentException("Dimensions do not agree in matrix multiply.");
	}
	// if this or other is non-real, die here
	if ((this.realFlag == false) || (other.realFlag == false)) {
	    throw new RuntimeException("complex matrix multiply not supported yet");
	}
	// just implement matrix multiply straight up, one element after another
	FilterMatrix product = new FilterMatrix(this.getRows(), other.getCols());
	// for each row of this
	for (int i=0; i<this.internalSizeRows; i++) {
	    // for each col of other
	    for (int j=0; j<other.internalSizeCols; j++) {
		double sum = 0;
		// for each element, compute partial sum (eg this(i,k)other(k,j)
		for (int k=0; k<this.internalSizeCols; k++) {
		    // sum = sum + (this(i,k)*other(k,j)
		    sum += this.internalMatrixReal[i][k]*other.internalMatrixReal[k][j];
		}
		// set the element i,j in the new matrix
		product.internalMatrixReal[i][j] = sum;
	    }
	}
	return product;
    }

    /**
     * Return the element-wise sum of this matrix with other.
     * Requires that this and other have the same dimensions.
     **/
    public FilterMatrix plus(FilterMatrix other) {
	if (other==null){throw new IllegalArgumentException("null other in plus()");}
	// check dimensions -- all must agree
	if ((this.internalSizeRows != other.internalSizeRows) ||
	    (this.internalSizeCols != other.internalSizeCols)) {
	    throw new IllegalArgumentException("Dimension mismatch in FilterMatrix.plus!");
	}
	
	// now, make the sum and add the vectors element wise
	FilterMatrix sum = new FilterMatrix(this.internalSizeRows, this.internalSizeCols);
	for (int i=0; i<this.internalSizeRows; i++) {
	    for (int j=0; j<this.internalSizeCols; j++) {
		sum.internalMatrixReal[i][j] = (this.internalMatrixReal[i][j] +
						other.internalMatrixReal[i][j]);
	    }
	}
	// if we have complex parts, sum them too
	if (this.realFlag == false) {
	    for (int i=0; i<this.internalSizeRows; i++) {
		for (int j=0; j<this.internalSizeCols; j++) {
		    sum.internalMatrixImag[i][j] = (this.internalMatrixImag[i][j] +
						    other.internalMatrixImag[i][j]);
		}
	    }
	}
	return sum;
    }	

    /**
     * Return the transpose of this FilterMatrix.
     */
    public FilterMatrix transpose() {
	// make a matrix with rows and cols swapped
	FilterMatrix result = new FilterMatrix(this.internalSizeCols, this.internalSizeRows);
	for (int i=0; i<this.internalSizeRows; i++) {
	    for (int j=0; j<this.internalSizeCols; j++) {
		result.internalMatrixReal[j][i] = this.internalMatrixReal[i][j];
	    }
	}
	// if we have imaginary parts, copy them too
	if (this.realFlag == false) {
	    for (int i=0; i<this.internalSizeRows; i++) {
		for (int j=0; j<this.internalSizeCols; j++) {
		    result.internalMatrixImag[j][i] = this.internalMatrixImag[i][j];
		}
	    }
	}
	return result;
    }

    /**
     * Return whether or not all the elements of this have integral
     * components.
     */
    public boolean isIntegral() {
	for (int i=0; i<internalSizeRows; i++) {
	    for (int j=0; j<internalSizeCols; j++) {
		// use the function in complex number...
		// Not so efficient, but easy to implement...
		ComplexNumber temp = new ComplexNumber(this.internalMatrixReal[i][j],
						       this.internalMatrixImag[i][j]);
		if (!temp.isIntegral()) {
		    return false;
		}
	    }
	}
	return true;
    }

    /**
     * Return whether or not all the elements of this FilterMatrix are real.
     */
    public boolean isReal() {
	// we can simply cheat and return our flag (that we keep up to date 
	// to see if this is a real flag or not.
	return this.realFlag;
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
	if (this.internalSizeRows != other.internalSizeRows) {return false;}
	if (this.internalSizeCols != other.internalSizeCols) {return false;}

	// now, compare element by element(real)
	for (int i=0; i<this.internalSizeRows; i++) {
	    for (int j=0; j<this.internalSizeCols; j++) {
		// if the elements are not the same, we are done
		// use the doubleEquals method from ComplexNumber
		if (!ComplexNumber.doubleEquals(this.internalMatrixReal[i][j],
						other.internalMatrixReal[i][j])) {
		    return false;
		}
	    }
	}
	// now, compare element by element(imag) if necessary
	if (this.realFlag == false) {
	    for (int i=0; i<this.internalSizeRows; i++) {
		for (int j=0; j<this.internalSizeCols; j++) {
		    // if the elements are not the same, we are done
		    // use the doubleEquals method from COmplexNumber
		    if (!ComplexNumber.doubleEquals(this.internalMatrixImag[i][j],
						    other.internalMatrixImag[i][j])) {
			return false;
		    }
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
     * are equal, they should produce the same hash code. Performance
     * will be bad in large programs, but correctness is guaranteed.
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
	// if the factor is complex, revery back to slow implementation
	if (!factor.isReal()) {	
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
	} else {
	    double realFactor = factor.getReal();
	    // factor is only real, so do things very fast
	    for (int i=0; i<this.internalSizeRows; i++) {
		for (int j=0; j<this.internalSizeCols; j++) {
		    this.internalMatrixReal[i][j] *= realFactor;
		}
	    }
	}	    
    }
    

    /** PrettyPrint our matrix. **/
    public String toString() {
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
		returnString += this.getElement(i,j);
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
     * Returns REAL PART of matrix with a line for each row; entries
     * between columns separated by a tab.
     */
    public String toTabSeparatedString() {
	StringBuffer sb = new StringBuffer();
	for (int i=0; i<internalSizeRows; i++) {
	    for (int j=0; j<internalSizeCols; j++) {
		// append real part
		sb.append(this.getElement(i,j).getReal());
		// add tab if not at end
		if (j!=(internalSizeCols-1)) {
		    sb.append("\t");
		}
	    }
	    // newline if not at end
	    if (i!=(internalSizeRows-1)) {
		sb.append("\n");
	    }
	}
	return sb.toString();
    }

    /**
     * Print only a "0" for zero elements and an "x" for non-zero elements.
     **/
    public String getZeroString() {
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
		if (this.getElement(i,j).equals(ComplexNumber.ZERO)) {
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
