package at.dms.kjc.sir.linear;

import java.util.*;

/**
 * A FilterVector is, at its most basic level, a simple, one dimensional
 * row vector. In the linear dataflow analysis, FilterVectors are used to
 * keep track of the combinations of inputs that are used to compute a 
 * specific intermediate value in the program flow.
 *
 * $Id: FilterVector.java,v 1.4 2002-09-06 17:19:42 aalamb Exp $
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
}
	


    
