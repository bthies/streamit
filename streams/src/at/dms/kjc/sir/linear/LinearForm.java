package at.dms.kjc.sir.linear;

import java.util.*;

/**
 * A LinearForm is the representation of a variable inside
 * the linear dataflow analysis. It is comprised of a vector and a value.
 * The vector corresponds to the combinations of inputs used to compute the value,
 * and the value corresponds to a constant that is added to compute the value.
 *
 * The basic gist of the dataflow analysis is to determine for all variables 
 * what the corresponding LinearForm is. Armed with this knowledge, we can
 * propage LinearForm information throughout the body of the filter and
 * hopefully construct a LinearFilterRepresentation from the filter.
 *
 * $Id: LinearForm.java,v 1.2 2002-08-15 20:38:04 aalamb Exp $
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
	return this.offset.isRealInteger();
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
    /** set the offset **/
    public void setOffset(int val) {this.setOffset(new ComplexNumber(val, 0));}
    /** set the offset **/
    public void setOffset(double val) {this.setOffset(new ComplexNumber(val, 0));}
    /** set the offset **/
    public void setOffset(ComplexNumber val) {this.offset = val;}
    
    /**
     * returns true if this LinearForm contains only a constant offset
     * and a vector of all zeros.
     **/
    public boolean isOnlyOffset() {
	for (int i=0; i<this.weights.getSize(); i++) {
	    if (!(ComplexNumber.ZERO.equals(this.weights.getElement(i)))) {
		// if the element was non zero, return false becasue there
		// was something in the weights vector
		return false;
	    }
	}
	return true;
    }    

    /** sets the weight of a particular item in the linear form **/
    public void setWeight(int index, ComplexNumber weight) {
	// try and set the item in the weights vector -- it takes
	// care of all of the error handling
	this.weights.setElement(index, weight);
    }
    /** gets a speficied weight **/
    public ComplexNumber getWeight(int index) {
	// let the internal matrix rep handle the error bounds checking
	return this.weights.getElement(index);
    }

    /**
     * Negate the LinearForm -- to do this, we reverse the sign of the
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

    

    /** Pretty print this linear form **/
    public String toString() {
	return ("Linear Form: (" +
		this.weights.toString() +
		" + " + this.offset + ")");
    }

    
}


