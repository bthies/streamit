package at.dms.kjc.sir.statespace;

/**
 * A LinearForm is the representation of a variable inside
 * the linear dataflow analysis. It is comprised of vectors v and w.
 * The vector v corresponds to the combinations of inputs used to compute the value,
 * and the vector w corresponds to the combinations of state variables that are used to compute the value.<br>
 *
 * The basic gist of the dataflow analysis is to determine for all variables 
 * what the corresponding LinearForm is. Armed with this knowledge, we can
 * propage LinearForm information throughout the body of the filter and
 * construct a LinearFilterRepresentation from the filter.<br>
 *
 * $Id: LinearForm.java,v 1.2 2004-02-12 22:32:57 sitij Exp $
 * Modified to state space form by Sitij Agrawal  2/9/04
 **/

public class LinearForm {
    /** weights of inputs **/
    private FilterVector v;
    /** weights of states **/
    private FilterVector w;

    /** Construct a new LinearForm with vector size size with all elements zero. **/
    public LinearForm(int inputs, int states) {
	this.v = new FilterVector(inputs);
	this.w = new FilterVector(states);
    }

    /** returns true if this linear form's offset is an integer **/   
    public boolean isIntegerOffset() {
        ComplexNumber off = this.getOffset();
	if (!off.isReal()) {
	    // offset is a complex number, not an integer
	    return false;
	}
	return off.isIntegral();
    }

    /** Get the constant offset of this linear form. **/
    public int getIntegerOffset() {
	if (!(this.isIntegerOffset())) {
	    throw new RuntimeException("you can't get the integer offset of a non integer!");
	}
        ComplexNumber off = this.getOffset();
	return ((int)off.getReal());
    }

    /** Get the constant offset of this linear form. **/    
    public ComplexNumber getOffset() {
        if(this.w.getSize() > 0)
	  return this.w.getElement(this.w.getSize()-1);
        else
	    return new ComplexNumber(0,0);
    }
    
    /** Set the offset with an integer. **/
     public void setOffset(int val) {this.setOffset(new ComplexNumber(val, 0));} 
    /** Set the offset with a double. **/
     public void setOffset(double val) {this.setOffset(new ComplexNumber(val, 0));} 
    /** Set the offset with a ComplexNumber. **/
     public void setOffset(ComplexNumber val) {w.setElement(w.getSize()-1,val);}
    
    /**
     * Returns true if this LinearForm contains only a constant offset
     * This means the inputs vector is all zeros, and the states vector is all zeros except for the last weight
     **/
    
    public boolean isOnlyOffset() {
	for (int i=0; i<this.v.getSize(); i++) {
	    if (!(ComplexNumber.ZERO.equals(this.v.getElement(i)))) {
		// if the element was non zero, return false because there
		// was something in the inputs vector
		return false;
	    }
	}
	for (int i=0; i<this.w.getSize()-1; i++) {
	    if (!(ComplexNumber.ZERO.equals(this.w.getElement(i)))) {
		// if the element was non zero, return false because there
		// was something in the states vector before the last entry
		return false;
	    }
	}       
	return true;
    }    
    

    /** Sets the weight of a particular item in the inputs vector. **/
    public void setInputWeight(int index, ComplexNumber weight) {
	// try and set the item in the appropriate vector -- it takes
	// care of all of the error handling
       this.v.setElement(index, weight);
    }


    /** Sets the weight of a particular item in the states vector. **/
    public void setStateWeight(int index, ComplexNumber weight) {
	// try and set the item in the appropriate vector -- it takes
	// care of all of the error handling
       this.w.setElement(index, weight);
    }

    /** Gets a specified input weight. **/
    public ComplexNumber getInputWeight(int index) {
	// let the internal matrix rep handle the error bounds checking
	return this.v.getElement(index);
    }

    /** Gets a specified state weight. **/
    public ComplexNumber getStateWeight(int index) {
	// let the internal matrix rep handle the error bounds checking
	return this.w.getElement(index);
    }

    /** Gets the internal size of the input vector of this linear form **/
    public int getInputWeightsSize() {
	return this.v.getSize();
    }

   /** Gets the internal size of the state vector of this linear form **/
    public int getStateWeightsSize() {
	return this.w.getSize();
    }

    /**
     * Negate the LinearForm. To do this, we reverse the sign of each element each vector.
     * This method creates a new LinearForm, and returns that as the
     * the result.
     **/
    public LinearForm negate() {
	// make a new linear form with the same size as this
	LinearForm negatedForm = new LinearForm(this.v.getSize(), this.w.getSize());

	// for each element in inputs vector, negate the value
	for (int i=0; i<this.v.getSize(); i++) {
	    negatedForm.setInputWeight(i, this.getInputWeight(i).negate());
	}

	// for each element in states vector, negate the value
	for (int i=0; i<this.w.getSize(); i++) {
	    negatedForm.setStateWeight(i, this.getStateWeight(i).negate());
	}
	// return the negated form
	return negatedForm;
    }

    /**
     * Add two linear forms together. To add two forms, you add each element in
     * each vector element-wise.
     * Returns a new linear form.
     **/
    public LinearForm plus(LinearForm other) {
	// ye olde requisite null pointer check
	if (other == null) {throw new IllegalArgumentException("null argument");}
	// check to make sure that this linear form is the same
	// size as this (otherwise don't allow the add)
	if ((other.v.getSize() != this.v.getSize())||(other.w.getSize() != this.w.getSize())) {
	    throw new IllegalArgumentException("sizes of linear forms don't match while adding.");
	}
	LinearForm summedForm = new LinearForm(this.v.getSize(), this.w.getSize());
	
	// for each of the weights, sum the input weights
	for(int i=0; i<this.v.getSize(); i++) {
	    summedForm.setInputWeight(i,this.getInputWeight(i).plus(other.getInputWeight(i)));
	}
	// for each of the state weights, sum the input weights
	for(int i=0; i<this.w.getSize(); i++) {
	    summedForm.setStateWeight(i,this.getStateWeight(i).plus(other.getStateWeight(i)));
	}
	// return the summed linear form
	return summedForm;
    }

    /**
     * Scale a linear form by a constant. This is used for multiplications
     * in the code by a constant factor. All of the vector entries
     * of the linear form are scaled by this constant.
     **/
    public LinearForm multiplyByConstant(ComplexNumber scaleFactor) {
	if (scaleFactor == null) {
	    throw new IllegalArgumentException("null scale factor");
	}
	LinearForm scaledForm = new LinearForm(this.v.getSize(), this.w.getSize());
	// copy the weights from this
	scaledForm.v = (FilterVector)this.v.copy();
	scaledForm.w = (FilterVector)this.w.copy();
	// actually scale the weights 
	scaledForm.v.scale(scaleFactor);
	scaledForm.w.scale(scaleFactor);
	return scaledForm;
    }

    /**
     * Divide all weights and the offset of this linear form by a constant.
     **/
    public LinearForm divideByConstant(ComplexNumber divideFactor) {
	if (divideFactor == null) {
	    throw new IllegalArgumentException("null divide factor");
	}
	// make a new linear form
	LinearForm dividedForm = new LinearForm(this.v.getSize(),this.w.getSize());
	// copy over the weights, one by one, dividing each one
	for (int i=0; i<this.v.getSize(); i++) {
	    dividedForm.setInputWeight(i,this.getInputWeight(i).dividedby(divideFactor));
	}
	for (int i=0; i<this.w.getSize(); i++) {
	    dividedForm.setStateWeight(i,this.getStateWeight(i).dividedby(divideFactor));
	}

	return dividedForm;
    }

    /**
     * Add all of the weights in this linear form to the specified row in
     * the passed FilterMatrix. Weights are copied from index 0 to index n of
     * the linear form, and from index (row,0) to index (row,n) of the filter matrix.
     * eg from left to right in linear form becomes left to right in
     * filter matrix. One more way -- copy the row vector in this linear form into the
     * row row of the filter matrix.<br>
     * 
     **/
    
    public void copyInputsToRow(FilterMatrix fm, int row) {
	if (fm==null) {throw new IllegalArgumentException("null to copyToRow");}
	// check to make sure that the row is valid
	if (fm.getRows() <= row) {
	    throw new IllegalArgumentException("row " + row +
					       " is an invalid row in filter matrix " + fm);
	}
	// make sure that we are the same size as the row we are trying to copy into.
	if (this.getInputWeightsSize() != fm.getCols()) {
	    throw new IllegalArgumentException("rows of filter matrix aren't the same as this linear form");
	}

	// just copy the elements in weights, element-wise
	int size = this.getInputWeightsSize();
	for (int i=0; i<size; i++) {
	    fm.setElement(row, i, this.v.getElement(i));
	}
	// and we are done
    }

    public void copyStatesToRow(FilterMatrix fm, int row) {
	if (fm==null) {throw new IllegalArgumentException("null to copyToRow");}
	// check to make sure that the row is valid
	if (fm.getRows() <= row) {
	    throw new IllegalArgumentException("row " + row +
					       " is an invalid row in filter matrix " + fm);
	}
	// make sure that we are the same size as the row we are trying to copy into.
	if (this.getStateWeightsSize() != fm.getCols()) {
	    throw new IllegalArgumentException("rows of filter matrix aren't the same as this linear form");
	}

	// just copy the elements in weights, element-wise
	int size = this.getStateWeightsSize();
	for (int i=0; i<size; i++) {
	    fm.setElement(row, i, this.w.getElement(i));
	}
	// and we are done
    }
    

    /** Returns true if this object is equal in value to this linear form. **/
    public boolean equals(Object o) {
	if (o == null) {return false;}
	if (!(o instanceof LinearForm)) {return false;}
	LinearForm other = (LinearForm)o;
	// compare both vectors
	return ((this.v.equals(other.v)) &&
		(this.w.equals(other.w)));
    }

    /** Preserve equals() semantics. **/
    public int hashCode() {
	return 1;
    }

    /** Pretty print this linear form. **/
    public String toString() {
	return ("Linear Form: (" +
		this.v.toString() +
		" + " + this.w.toString() + ")");
    }
}


