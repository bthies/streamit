package at.dms.kjc.sir.linear;

/**
 * This class represents a complex number in the Linear filter
 * extraction framework. It seems as though this should eventually be a
 * first class citizen of the IR, but for now we'll convert back and
 * forth between this and the structure that exists in the language.
 *
 * Complex numbers are immutable -- eg their value can't change after
 * they are instantiated.
 *
 * $Id: ComplexNumber.java,v 1.4 2002-08-15 20:38:04 aalamb Exp $
 **/
public class ComplexNumber {
    private final double realPart;
    private final double imaginaryPart;

    /**
     * the maximum difference between two complex numbers (either their real or imaginary parts)
     * before the equals method will not distinguish them anymore.
     **/
    public static final double MAX_PRECISION = .0000000001;
    
    
    /** Canonical number zero **/
    public static final ComplexNumber ZERO = new ComplexNumber(0,0);
    /** Canonical real number one **/
    public static final ComplexNumber ONE  = new ComplexNumber(1,0);

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
    /** returns true of abs(imaginary part) is less than MAX_PRECISION **/
    public boolean isReal() {return (Math.abs(this.imaginaryPart) < MAX_PRECISION);}
    /** returns true if the real part of this is an integer **/
    public boolean isRealInteger() { return (Math.floor(this.realPart) == this.realPart);}

    /////// Arithemetic Operations
    /** compute the negative of this complex number **/
    public ComplexNumber negate() {
	return new ComplexNumber(-1*this.realPart, -1*this.imaginaryPart);
    }
    /** Compute the sum of this and the passed complex number **/
    public ComplexNumber plus(ComplexNumber other) {
	if (other == null) {throw new IllegalArgumentException("Null object passed to add");}
	return new ComplexNumber(this.realPart + other.realPart,
				 this.imaginaryPart + other.imaginaryPart);
    }


    
    /** Return true if the passed complex number is the same as this (by value) **/
    public boolean equals(Object o) {
	if (o == null) {
	    throw new RuntimeException("Null object passed to ComplexNumber.equals");
	}
	if (!(o instanceof ComplexNumber)) {
	    return false;
	}
	ComplexNumber other = (ComplexNumber)o;
	//return ((other.getReal() == this.getReal()) &&
	//(other.getImaginary() == this.getImaginary()));

	// we get into precision issues, so we ignore any differences after the 10th decimal place.
	double realDiff = Math.abs(other.realPart - this.realPart);
	double imagDiff = Math.abs(other.imaginaryPart - this.imaginaryPart);

	return ((realDiff < MAX_PRECISION) &&
		(imagDiff < MAX_PRECISION));	
    }

    /** hashcode so that data structures work correctly **/
    public int hashCode() {
	return 1;
    }

    /** PrettyPrint this complex number. **/
    public String toString() {
	return (this.getReal() + "+" + this.getImaginary() + "i");
    }
    
}
