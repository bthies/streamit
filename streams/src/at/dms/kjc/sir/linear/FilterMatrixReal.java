package at.dms.kjc.sir.linear;

import java.util.*;

/**
 * Special case of a FilterMatrix where each element is
 * a real floating point number. This implementation is
 * necessary to speedup performance (FilterMatrix.times is
 * wicked slow).
 *
 * Each element of the FilterMatrix is a ComplexNumber
 *
 * $Id: FilterMatrixReal.java,v 1.1 2003-04-11 00:14:30 aalamb Exp $
 **/

public class FilterMatrixReal extends FilterMatrix {
    /** internal rep of the real matrix. **/
    private double[][] realCoefficients;
    private int realRows;
    private int realCols;

    /** make a new matrix with all zero coefficients. **/
    public FilterMatrixReal(int rows, int cols) {
	if (rows < 0) {throw new IllegalArgumentException("rows < 0");}
	if (cols < 0) {throw new IllegalArgumentException("cols < 0");}
	this.realRows = rows;
	this.realCols = cols;
	this.realCoefficients = new double[rows][cols];
    }
    /**
     * make a new matrix with the same values as mat. Throws an
     * exception if there any non-real coefficients in mat.
     **/
    public FilterMatrixReal(FilterMatrix mat) {
	this.realRows = mat.getRows();
	this.realCols = mat.getCols();
	this.realCoefficients = new double[this.realRows][this.realCols];
	// copy over the elements from mat into this.realCoefficients
	for (int i=0; i<this.realRows; i++) {
	    for (int j=0; j<this.realCols; j++) {
		ComplexNumber currentNumber = mat.getElement(i,j);
		if (!currentNumber.isReal()) {
		    throw new IllegalArgumentException("non real item (" +
						       i + "," + j + ")");
		} else {
		    this.realCoefficients[i][j] = (double)currentNumber.getReal();
		}
	    }
	}
    }
}
