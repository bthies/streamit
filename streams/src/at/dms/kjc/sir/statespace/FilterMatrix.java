package at.dms.kjc.sir.statespace;

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
 * $Id: FilterMatrix.java,v 1.7 2004-03-12 23:48:28 sitij Exp $
 **/

public class FilterMatrix {
    /** Internal representation of the matrix (real). **/ 
    private double internalMatrixReal[][] = null;
    /** Internal representation of the matrix (imag). **/
    private double internalMatrixImag[][] = null;
    /** This flag is used to easily differentiate between real and complex matrices. **/
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
	assert this.isReal();
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

    /** Return the determinant of this matrix (if it is square and real) **/

    public double determinant() {
	
	if(this.getRows()!=this.getCols())
	    throw new IllegalArgumentException("can't take determinant of non-square matrix");

	if(this.realFlag==false)
	    throw new IllegalArgumentException("won't do determinant of complex matrix");

	int length = this.getRows();

	if(length==1)
	    return this.getElement(0,0).getReal();

	double sum, factor;
	sum = 0.0;
	FilterMatrix temp = new FilterMatrix(length-1,length-1);
	temp.copyRowsAndColsAt(0,0,this,1,1,length-1,length-1);

	//go along the first row of the matrix
	for(int i=0; i < length; i++) {

	    factor = this.getElement(0,i).getReal();
	    factor *= temp.determinant();
	    if((i % 2)==1)
		factor *= -1.0;
	    sum += factor;

	    if(i<length-1)
		temp.copyRowsAndColsAt(0,i,this,1,i,length-1,1);

	}

	return sum;

    }


    /** Returns the inverse of this matrix, if it exists **/

    public FilterMatrix inverse() {
	
	double det;

	try {
	    det = this.determinant();
	}
	catch(IllegalArgumentException e) {
	    throw new IllegalArgumentException("Cannot take inverse of this matrix: " + e);
	}

	if(det==0.0)
	    throw new IllegalArgumentException("Cannot take inverse of this matrix: determinant is zero");

	int length = this.getRows();
	FilterMatrix returnMatrix = new FilterMatrix(length,length);

	if(length==1) {
	    double oldVal = this.getElement(0,0).getReal();
	    returnMatrix.setElement(0,0, new ComplexNumber(1/oldVal,0));
	}
	else {

	    FilterMatrix temp = new FilterMatrix(length-1,length);
	    FilterMatrix temp2 = new FilterMatrix(length-1,length-1);
	    double factor;

	    for(int i=0; i < length; i++) {
		
		if(i > 0)
		    temp.copyRowsAndColsAt(0,0,this,0,0,i,length);

		if(i < length-1)
		    temp.copyRowsAndColsAt(i,0,this,i+1,0,length-i-1,length);

		temp2.copyColumnsAt(0,temp,1,length-1);


		for(int j=0; j < length; j++) {


		    LinearPrinter.println("i,j,minor: " + i + " " + j + " " + temp2);
		 
		    factor = temp2.determinant() / det;
		    if(((i+j) % 2)==1)
			factor *= -1;

		    returnMatrix.setElement(j,i, new ComplexNumber(factor,0));

		    if(j < length-1)
			temp2.copyColumnsAt(j,temp,j,1);


		}
	    }
	}

	return returnMatrix;
    }



    /** swap row1, row2 with each other */

    public void swapRows(int row1, int row2) {

	int totalRows = this.getRows();

	if((row1 < 0)||(row1 >= totalRows)) 
	   throw new IllegalArgumentException("invalid parameter row1 = " + row1);
	if((row2 < 0)||(row2 >= totalRows)) 
	   throw new IllegalArgumentException("invalid parameter row2 = " + row2);

	int totalCols = this.getCols();
	double swap_r, swap_i;

	for(int j=0; j<totalCols; j++) {
	    swap_r = this.internalMatrixReal[row1][j];
	    this.internalMatrixReal[row1][j] = this.internalMatrixReal[row2][j];
	    this.internalMatrixReal[row2][j] = swap_r;
	}

	if(!this.realFlag) {
	    for(int j=0; j<totalCols; j++) {
		swap_i = this.internalMatrixImag[row1][j];
		this.internalMatrixImag[row1][j] = this.internalMatrixImag[row2][j];
		this.internalMatrixImag[row2][j] = swap_i;
	    }
	}
    }



    /** swap col1, col2 with each other */

    public void swapCols(int col1, int col2) {

	int totalCols = this.getCols();

	if((col1 < 0)||(col1 >= totalCols)) 
	   throw new IllegalArgumentException("invalid parameter col1 = " + col1);
	if((col2 < 0)||(col2 >= totalCols)) 
	   throw new IllegalArgumentException("invalid parameter col2 = " + col2);

	int totalRows = this.getRows();
	double swap_r, swap_i;

	for(int i=0; i<totalRows; i++) {
	    swap_r = this.internalMatrixReal[i][col1];
	    this.internalMatrixReal[i][col1] = this.internalMatrixReal[i][col2];
	    this.internalMatrixReal[i][col2] = swap_r;
	}
	if(!realFlag) {
	    for(int i=0; i<totalRows; i++) {
		swap_i = this.internalMatrixImag[i][col1];
		this.internalMatrixImag[i][col1] = this.internalMatrixImag[i][col2];
		this.internalMatrixImag[i][col2] = swap_i;
	    }
	}
    }


    /** swap rows val1, val2 and swap cols val1, val2 - Note that order does not matter **/

    public void swapRowsAndCols(int val1, int val2) {

	int totalRows = this.getRows();
	int totalCols = this.getCols();

	// do checks in case swapRows works but swapCols doesn't

	if((val1 < 0)||(val1 >= totalRows)||(val1 >= totalCols))
	    throw new IllegalArgumentException("invalid parameter val1 = " + val1);
	if((val2 < 0)||(val2 >= totalRows)||(val2 >= totalCols))
	    throw new IllegalArgumentException("invalid parameter val2 = " + val2);

	swapRows(val1,val2);
	swapCols(val1,val2);
    }


    /** multiplies row by specified scalar **/

    public void multiplyRow(int row, double scalar) {

	int totalRows = this.getRows();

       	if((row < 0)||(row >= totalRows))
	    throw new IllegalArgumentException("invalid parameter row = " + row);

	int totalCols = this.getCols();

	for(int j=0; j<totalCols; j++) 
	    this.internalMatrixReal[row][j] *= scalar;

	if(!this.realFlag) {
	    for(int j=0; j<totalCols; j++) {
		this.internalMatrixImag[row][j] *= scalar;
	    }
	}
    }


   /** multiplies column by specified scalar **/

    public void multiplyCol(int col, double scalar) {

	int totalCols = this.getCols();

       	if((col < 0)||(col >= totalCols))
	    throw new IllegalArgumentException("invalid parameter col = " + col);

	int totalRows = this.getRows();

	for(int i=0; i<totalRows; i++) 
	    this.internalMatrixReal[i][col] *= scalar;

	if(!this.realFlag) {
	    for(int i=0; i<totalRows; i++) {
		this.internalMatrixImag[i][col] *= scalar;
	    }
	}
    }

    /** multiplies row and col by specified scalar **/

    public void multiplyRowAndCol(int val, double scalar) {

	int totalRows = this.getRows();
	int totalCols = this.getCols();

       	if((val < 0)||(val >= totalRows))
	    throw new IllegalArgumentException("Parameter val exceeds row bounds = " + val);

       	if((val < 0)||(val >= totalCols))
	    throw new IllegalArgumentException("Parameter val exceeds col bounds = " + val);
	
	this.multiplyRow(val,scalar);
	this.multiplyCol(val,scalar);
    }



    /** add scalar*row1 to row2 **/

    public void addRow(int row1, int row2, double scalar) {

	int totalRows = this.getRows();

	if((row1 < 0)||(row1 >= totalRows)) 
	   throw new IllegalArgumentException("invalid parameter row1 = " + row1);
	if((row2 < 0)||(row2 >= totalRows)) 
	   throw new IllegalArgumentException("invalid parameter row2 = " + row2);

	int totalCols = this.getCols();

	for(int j=0; j<totalCols; j++) 
	    this.internalMatrixReal[row2][j] += scalar*this.internalMatrixReal[row1][j];

	if(!this.realFlag) {
	    for(int j=0; j<totalCols; j++) {
		this.internalMatrixImag[row2][j] += scalar*this.internalMatrixImag[row1][j];
	    }
	}
    }


    /** add scalar*col1 to col2 **/

    public void addCol(int col1, int col2, double scalar) {

	int totalCols = this.getCols();

	if((col1 < 0)||(col1 >= totalCols)) 
	   throw new IllegalArgumentException("invalid parameter col1 = " + col1);
	if((col2 < 0)||(col2 >= totalCols)) 
	   throw new IllegalArgumentException("invalid parameter col2 = " + col2);

	int totalRows = this.getRows();

	for(int i=0; i<totalRows; i++) 
	    this.internalMatrixReal[i][col2] += scalar*this.internalMatrixReal[i][col1];

	if(!this.realFlag) {
	    for(int i=0; i<totalCols; i++) {
		this.internalMatrixImag[i][col2] += scalar*this.internalMatrixImag[i][col1];
	    }
	}
    }


    /** add scalar * row val1 to col val2 and scalar * col val1 to col val2 **/

    public void AddRowAndCol(int val1, int val2, double scalar) {

	int totalRows = this.getRows();
	int totalCols = this.getCols();

	if((val1 < 0)||(val1 >= totalRows)||(val1 >= totalCols))
	    throw new IllegalArgumentException("invalid parameter val1 = " + val1);
	if((val2 < 0)||(val2 >= totalRows)||(val2 >= totalCols))
	    throw new IllegalArgumentException("invalid parameter val2 = " + val2);

	this.addRow(val1,val2,scalar);
	this.addCol(val1,val2,scalar);

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
     * Copied numRows, numCols at the thisOffset positions of this FilterMatrix
     * from sourceOffsets of the source filter matrix.
     **/
    public void copyRowsAndColsAt(int thisRowOffset, int thisColOffset, FilterMatrix source, int sourceRowOffset, int sourceColOffset, int numRows, int numCols) { 

	if((numRows > 0)&&(numCols > 0)) {
	    FilterMatrix tempMatrix = new FilterMatrix(numRows,source.getCols());
	    tempMatrix.copyRowsAt(0,source,sourceRowOffset,numRows);
	    FilterMatrix tempMatrix2 = new FilterMatrix(numRows,numCols);
	    tempMatrix2.copyColumnsAt(0,tempMatrix,sourceColOffset,numCols);
	    this.copyAt(thisRowOffset,thisColOffset,tempMatrix2);
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

    /* returns true if all elements are zero */
    public boolean isZero() {

	for (int i=0; i<internalSizeRows; i++) {	
	    for (int j=0; j<internalSizeCols; j++) {
		if (!(this.getElement(i,j).equals(ComplexNumber.ZERO)))
		    return false;
	    }
	}
	return true;
    }

    /**
     * Returns the the first row of zeros, or -1 if there is no such row
     */

    public int zeroRow() {
	boolean found = true;
	for (int i=0; i<internalSizeRows; i++) {	
	    for (int j=0; j<internalSizeCols; j++) {
		if (!(this.getElement(i,j).equals(ComplexNumber.ZERO)))
		    found = false;
	    }
	    if(found)
		return i;
	}
	return -1;
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




