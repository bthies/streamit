package at.dms.kjc.sir.statespace;

/**
 * A LinearForm is the representation of a variable inside
 * the linear dataflow analysis. It is comprised of a vector v and a value c.
 * The vector corresponds to the combinations of inputs used to compute the value,
 * and the value corresponds to a constant that is added to compute the value.<br>
 *
 * The basic gist of the dataflow analysis is to determine for all variables 
 * what the corresponding LinearForm is. Armed with this knowledge, we can
 * propage LinearForm information throughout the body of the filter and
 * construct a LinearFilterRepresentation from the filter.<br>
 *
 * $Id: LinearForm.java,v 1.1 2004-02-09 17:55:01 thies Exp $
 **/
public class LinearForm {
    /** weights of inputs **/
    private FilterVector weights;
    /** offset that is added later **/
    private ComplexNumber offset;

    /** Construct a new LinearForm with vector size size with all elements zero. **/
    public LinearForm(int size) {
	this.weights = new FilterVector(size);
	this.offset = ComplexNumber.ZERO;
    }

    /** returns true if this linear form's offset is an integer **/
    public boolean isIntegerOffset() {
	if (!this.offset.isReal()) {
	    // offset is a complex number, not an integer
	    return false;
	}
	return this.offset.isIntegral();
    }

    /** Get the constant offset of this linear form. **/
    public int getIntegerOffset() {
	if (!(this.isIntegerOffset())) {
	    throw new RuntimeException("you can't get the integer offset of a non integer!");
	}
	return ((int)this.offset.getReal());
    }
    /** Get the constant offset of this linear form. **/
    public ComplexNumber getOffset() {
	return this.offset;
    }
    /** Set the offset with an integer. **/
    public void setOffset(int val) {this.setOffset(new ComplexNumber(val, 0));}
    /** Set the offset with a double. **/
    public void setOffset(double val) {this.setOffset(new ComplexNumber(val, 0));}
    /** Set the offset with a ComplexNumber. **/
    public void setOffset(ComplexNumber val) {this.offset = val;}
    
    /**
     * Returns true if this LinearForm contains only a constant offset
     * and a vector of all zeros.
     **/
    public boolean isOnlyOffset() {
	for (int i=0; i<this.weights.getSize(); i++) {
	    if (!(ComplexNumber.ZERO.equals(this.weights.getElement(i)))) {
		// if the element was non zero, return false because there
		// was something in the weights vector
		return false;
	    }
	}
	return true;
    }    

    /** Sets the weight of a particular item in the linear form. **/
    public void setWeight(int index, ComplexNumber weight) {
	// try and set the item in the weights vector -- it takes
	// care of all of the error handling
	this.weights.setElement(index, weight);
    }
    /** Gets a speficied weight. **/
    public ComplexNumber getWeight(int index) {
	// let the internal matrix rep handle the error bounds checking
	return this.weights.getElement(index);
    }
    /** Gets the internal size of the vecotr of this linear form (i.e. the peek amount). **/
    public int getWeightsSize() {
	return this.weights.getSize();
    }

    /**
     * Negate the LinearForm. To do this, we reverse the sign of the
     * offset reverse the sign of each element in the array.
     * This method creates a new LinearForm, and returns that as the
     * the result.
     **/
    public LinearForm negate() {
	// make a new linear form with the same size as this
	LinearForm negatedForm = new LinearForm(this.weights.getSize());
	// negate the offset
	negatedForm.setOffset(this.getOffset().negate());
	// for each element, negate the offset
	for (int i=0; i<this.weights.getSize(); i++) {
	    negatedForm.setWeight(i, this.getWeight(i).negate());
	}
	// return the negated form
	return negatedForm;
    }

    /**
     * Add two linear forms together. To add two forms, you add each element in
     * the weights vector element-wise, and you add the offset.
     * Returns a new linear form.
     **/
    public LinearForm plus(LinearForm other) {
	// ye olde requisite null pointer check
	if (other == null) {throw new IllegalArgumentException("null argument");}
	// check to make sure that this linear form is the same
	// size as this (otherwise don't allow the add)
	if (other.weights.getSize() != this.weights.getSize()) {
	    throw new IllegalArgumentException("sizes of linear forms don't match while adding.");
	}
	LinearForm summedForm = new LinearForm(this.weights.getSize());
	// sum the offsets
	summedForm.setOffset(this.getOffset().plus(other.getOffset()));
	// for each of the weights, sum the weights
	for(int i=0; i<this.weights.getSize(); i++) {
	    summedForm.setWeight(i,this.getWeight(i).plus(other.getWeight(i)));
	}
	// return the summed linear form
	return summedForm;
    }

    /**
     * Scale a linear form by a constant. This is used for multiplications
     * in the code by a constant factor. All of the vector entries as well
     * as the constant of the linear form are scaled by this constant.
     **/
    public LinearForm multiplyByConstant(ComplexNumber scaleFactor) {
	if (scaleFactor == null) {
	    throw new IllegalArgumentException("null scale factor");
	}
	LinearForm scaledForm = new LinearForm(this.getWeightsSize());
	// copy the weights from this
	scaledForm.weights = (FilterVector)this.weights.copy();
	// actually scale the weights 
	scaledForm.weights.scale(scaleFactor);
	// scale the offset
	scaledForm.offset = this.offset.times(scaleFactor);
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
	LinearForm dividedForm = new LinearForm(this.getWeightsSize());
	// copy over the weights, one by one, dividing each one
	for (int i=0; i<this.getWeightsSize(); i++) {
	    dividedForm.setWeight(i,this.getWeight(i).dividedby(divideFactor));
	}
	// copy over the divided offset.
	dividedForm.setOffset(this.getOffset().dividedby(divideFactor));

	return dividedForm;
    }

    /**
     * Add all of the weights in this linear form to the specified column in
     * the passed FilterMatrix. Weights are copied from index 0 to index n of
     * the linear form, and from index (0,col) to index (n,col) of the filter matrix.
     * eg from left to right in linear form becomes top to bottom in
     * filter matrix. One more way -- copy the row vector in this linear form into the
     * col column of the filter matrix.<br>
     *
     * Note that the offset of the linear form is <b>not</b> copied anywhere.
     **/
    public void copyToColumn(FilterMatrix fm, int col) {
	if (fm==null) {throw new IllegalArgumentException("null to copyToColumn");}
	// check to make sure that the column is valid
	if (fm.getCols() <= col) {
	    throw new IllegalArgumentException("column " + col +
					       " is an invalid column in filter matrix " + fm);
	}
	// make sure that we are the same size as the column we are trying to copy into.
	if (this.getWeightsSize() != fm.getRows()) {
	    throw new IllegalArgumentException("cols of filter matrix aren't the same as this linear form");
	}

	// just copy the elements in weights, element-wise
	int size = this.getWeightsSize();
	for (int i=0; i<size; i++) {
	    fm.setElement(i,col, this.weights.getElement(i));
	}
	// and we are done
    }

    /** Returns true if this object is equal in value to this linear form. **/
    public boolean equals(Object o) {
	if (o == null) {return false;}
	if (!(o instanceof LinearForm)) {return false;}
	LinearForm other = (LinearForm)o;
	// compare both the weights and the
	// offset
	return ((this.offset.equals(other.offset)) &&
		(this.weights.equals(other.weights)));
    }

    /** Preserve equals() semantics. **/
    public int hashCode() {
	return 1;
    }

    /** Pretty print this linear form. **/
    public String toString() {
	return ("Linear Form: (" +
		this.weights.toString() +
		" + " + this.offset + ")");
    }
}


