package at.dms.kjc.linear;

/**
 * This class represents a complex number in the Linear filter
 * extraction framework. It seems as though this should eventually be a
 * first class citizen of the IR, but for now we'll convert back and
 * forth between this and the structure that exists in the language.
 *
 * Complex numbers are immutable -- eg their value can't change after
 * they are instantiated.
 **/
public class ComplexNumber {
    private final double realPart;
    private final double imaginaryPart;
    
    /** Create a complex number with real part re and imaginary part im **/
    public ComplexNumber(double re, double im) {
	this.realPart = re;
	this.imaginaryPart = im;
    }

    /////// Accessors
    /** Get the real part of this complex number. **/
    public double getReal() {return this.realPart;}
    /** Get the imaginary part of this complex number. **/
    public double getImaginary() {return this.imaginaryPart;}


    



    
    /** Return true if the passed complex number is the same as this (by value) **/
    public boolean equals(Object o) {
	if (o == null) {
	    throw new RuntimeException("Null object passed to ComplexNumber.equals");
	}
	if (!(o instanceof ComplexNumber)) {
	    return false;
	}
	ComplexNumber other = (ComplexNumber)o;
	return ((other.getReal() == this.getReal()) &&
		(other.getImaginary() == this.getImaginary()));
    }

    /** hashcode so that data structures work correctly **/
    public int hashCode() {
	return 1;
    }
    
}
