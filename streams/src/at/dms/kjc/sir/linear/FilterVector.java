package at.dms.kjc.sir.linear;

import java.util.*;

/**
 * A FilterVector is, at its most basic level, a simple, one dimensional
 * row vector. In the linear dataflow analysis, FilterVectors are used to
 * keep track of the combinations of inputs that are used to compute a 
 * specific intermediate value in the program flow. The fact that this is
 * implemented as a row vector and not a column vector is sort of
 * immaterial.<br>
 *
 * $Id: FilterVector.java,v 1.6 2003-06-02 15:09:39 aalamb Exp $
 **/

public class FilterVector extends FilterMatrix {
    /** Creates a vector of size i **/
    public FilterVector(int size) {
	// just make a matrix with one row and size cols
	super(1,size);
    }

    //////// Accessors/Modifiers
    
    
    /** Get the element at index in the vector **/
    public ComplexNumber getElement(int index) {
	//access via the superclass
	return super.getElement(0,index);
    }

    /** Sets the element at position index to be the specified complex number **/
    public void setElement(int index, ComplexNumber value) {
	// use the superclass's version
	super.setElement(0,index,value);
    }


    /** Gets the size of the vector **/
    public int getSize() {
	// this is very simple -- just the size of a row vector is
	//the number of columns
	return this.getCols();
    }

    /** return a copy of this filter vector. **/
    public FilterMatrix copy() {
	FilterVector fv = new FilterVector(this.getSize());
	// copy weights
	for (int i=0; i<this.getSize(); i++) {
	    fv.setElement(i,this.getElement(i));
	}
	return fv;
    }

    /**
     * Converts the matrix to a (row) vector (assuming that the matrix is actually a
     * row vector.) Throws an IllegalArgumentException if the matrix is not the appropriate size.
     * This method exists because the class heirarchy seems to be wrong. I wanted to reuse most of
     * the code from FilterMatrix (but a lot of the methods actually make new FilterMatrices,
     * like times() for instance.
     **/
    public static FilterVector toVector(FilterMatrix matrix) {
	if (matrix==null) {throw new IllegalArgumentException("null matrix to toVector()");}
	// ensure that the matrix is actually a row vector
	if (matrix.getRows() != 1) {
	    throw new IllegalArgumentException("matrix is not a row vector in toVector()");
	}
 	FilterVector newVector = new FilterVector(matrix.getCols());
	// copy over the elements in the matrix
	for (int i=0; i<matrix.getCols(); i++) {
	    newVector.setElement(i, matrix.getElement(0,i));
	}
	return newVector;
    }
}
	


    
