package at.dms.kjc.sir.linear;

//import java.io.*;
//import at.dms.compiler.JavaStyleComment;
//import at.dms.compiler.JavadocComment;
//import at.dms.util.Utils;
//import at.dms.kjc.sir.*;
//import at.dms.util.*;
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
 * $Id: FilterMatrix.java,v 1.5 2002-08-16 21:16:49 aalamb Exp $
 **/

public class FilterMatrix {
    /** Internal representation of the matrix **/ 
    private ComplexNumber internalMatrix[][] = null;
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
 	this.internalMatrix = new ComplexNumber[rows][cols];
	this.internalSizeRows = rows;
	this.internalSizeCols = cols;
	// initialize all to 0 
	for (int i=0; i<rows; i++) {
	    for (int j=0; j<cols; j++) {
		this.internalMatrix[i][j] = new ComplexNumber(0,0);
	    }
	}
	checkRep();
    }


    /**
     * Accessor: returns the value of the matrix at the specified position,
     * and bombs an exception if the value is out of range.
     **/
    public ComplexNumber getElement(int row, int col) {
	checkRep();
	// do bounds checking
	validateBounds(row,col);
	// actually give back the value
	return this.internalMatrix[row][col];
    }

    /**
     * Accessor: Return the number of rows in this matrix.
     **/
    public int getRows() {
	checkRep();
	return this.internalSizeRows;
    }
    /**
     * Accessor: Return the number of columns in this matrix.
     **/
    public int getCols() {
	checkRep();
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
	checkRep();
    }

    /**
     * Ensure that the specified row and col are within the
     * internal matrix's size. Throw an exception if they are not.
     **/
    private void validateBounds(int row, int col) {
     	if (row < 0) {
	    throw new IllegalArgumentException("Row " + row + " is less than one.");
	}
	if (col < 0) {
	    throw new IllegalArgumentException("Column " + col + " is less than one.");
	}
	if (row >= this.internalSizeRows) {
	    throw new IllegalArgumentException("Row " + row + " is greater than size:" +
					       "(" + this.internalSizeRows + "," +
					       this.internalSizeCols + ")");
	}
	if (col >= this.internalSizeCols) {
	    throw new IllegalArgumentException("Column " + col + " is greater than size:" +
					       "(" + this.internalSizeRows + "," +
					       this.internalSizeCols + ")");
	}

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
	checkRep();
	String returnString = "[";
	// for each row
	for (int i=0; i<internalSizeRows; i++) {
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
	}
	returnString += "]";

	return returnString;
    }


    
}
