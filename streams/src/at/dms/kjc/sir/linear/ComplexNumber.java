package at.dms.kjc.sir.linear;

/**
 * This class represents a complex number in the Linear filter
 * extraction framework. It seems as though this should eventually be a
 * first class citizen of the IR, but they are not. For now, we'll convert back and
 * forth between this and the structure that exists in the language
 * (i.e. JFloatLiteral).<br>
 *
 * Complex numbers are immutable -- i.e. their value can't change after
 * they are instantiated.<br>
 *
 * $Id: ComplexNumber.java,v 1.11 2003-06-02 15:09:39 aalamb Exp $
 **/
public class ComplexNumber {
    private final double realPart;
    private final double imaginaryPart;

    /**
     * The maximum difference between two complex numbers
     * (either their real or imaginary parts)
     * before the equals method will not distinguish them anymore.
     * This is necessary because because of the imprecision of
     * floating point arithmetic.
     **/
    public static final double MAX_PRECISION = .0000000001;
    
    
    /** Canonical number zero. **/
    public static final ComplexNumber ZERO = new ComplexNumber(0,0);
    /** Canonical real number one. **/
    public static final ComplexNumber ONE  = new ComplexNumber(1,0);

    /** Create a complex number with real part re and imaginary part im. **/
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
    public boolean isReal() {return (Math.abs(this.getImaginary()) < MAX_PRECISION);}
    /** returns true if both the real and imaginary parts of this are integers **/
    public boolean isIntegral() { 
	return ( Math.abs(Math.abs(Math.round(this.getReal())) -
			  Math.abs(this.getReal())) < MAX_PRECISION &&
		 Math.abs(Math.abs(Math.round(this.getImaginary())) -
			  Math.abs(this.getImaginary())) < MAX_PRECISION );
    }

    /////// Arithemetic Operations
    /** Compute the negative of this complex number. **/
    public ComplexNumber negate() {
	return new ComplexNumber(-1*this.getReal(), -1*this.getImaginary());
    }
    /** Compute the sum of this and the passed complex number. **/
    public ComplexNumber plus(ComplexNumber other) {
	if (other == null) {throw new IllegalArgumentException("Null object passed to plus");}
	return new ComplexNumber(this.getReal() + other.getReal(),
				 this.getImaginary() + other.getImaginary());
    }
    /** Multiply this by the specified complex number, and return their product. **/
    public ComplexNumber times(ComplexNumber other) {
	if (other == null) {throw new IllegalArgumentException("Null object passed to times");}
	// implement multiplication in the straightforward way.
	// real part
	double newReal = ((this.getReal() * other.getReal()) -
			  (this.getImaginary() * other.getImaginary()));
	// imag part
	double newImag = ((this.getReal() * other.getImaginary()) +
			  (this.getImaginary() * other.getReal()));
	return new ComplexNumber(newReal, newImag);
    }
    /** Divide this by the specified complex number and return the dividend. **/
    public ComplexNumber dividedby (ComplexNumber other) {
	if (other == null) {
	    throw new IllegalArgumentException("Null object passed to divide");
	}
	// simple implementation of division. First, we multiply the
	// both the numerator and denominator 
	// by the congugate of the denominator (makes the denom a real number),
	// then we can divide both the real
	// and imaginary part of the numerator by the constant in the bottom.
	ComplexNumber numerator = this;
	ComplexNumber denominator = other;

	// bomb an error if the denominator is zero
	if (denominator.equals(ComplexNumber.ZERO)) {
	    throw new IllegalArgumentException("Complex divide by zero");
	}
	
	// get the complex congugate of the denominator
	ComplexNumber conjugateOfDenominator = denominator.conjugate();
	// multiply numerator and denominator
	numerator = numerator.times(conjugateOfDenominator);
	denominator = denominator.times(conjugateOfDenominator);

	// make sure that the denominator has zero imaginary part
	if (!(denominator.getImaginary() == 0)) {
	    throw new RuntimeException("real part of denominator is non zero: " +
				       denominator.getImaginary());
	}

	// scale the numerator by 1/denom.
	numerator = numerator.times(new ComplexNumber(1/denominator.getReal(), 0));
	return numerator;
    }
    /**
     * Returns the complex congugate of this complex number.
     * i.e. reverse the sign of the imaginary part).
     **/
    public ComplexNumber conjugate() {
	return new ComplexNumber(this.getReal(),
				 -1*this.getImaginary());
    }
    
    /** Return true if the passed complex number is the same as this (by value). **/
    public boolean equals(Object o) {
	if (o == null) {
	    throw new RuntimeException("Null object passed to ComplexNumber.equals");
	}
	if (!(o instanceof ComplexNumber)) {
	    return false;
	}
	ComplexNumber other = (ComplexNumber)o;

	return (doubleEquals(other.getReal(), this.getReal()) &&
		doubleEquals(other.getImaginary(), this.getImaginary()));
    }
    
    /** Returns true if these two doubles are equal within a certain precision. **/
    public static boolean doubleEquals(double d1, double d2) {
	double diff = Math.abs(d1 - d2);
	return (diff < MAX_PRECISION);
    }
    
    /** Hashcode so that data structures work correctly **/
    public int hashCode() {
	return 1;
    }

    /** PrettyPrint this complex number. **/
    public String toString() {
	return (this.getReal() + "+" + this.getImaginary() + "i");
    }	    

    /**
     * This class represents a root of unity of the
     * form e^((-2*pi/k)*n) where n and k are parameters.
     * This class is currently unused -- originally it was meant for
     * automatically teasing out of a program where it was doing a DSP
     * transform and then feeding that information into spiral
     * (which has a primitive that is the root of unity which it uses
     * to derive its factorizations).
     **/
    static class RootOfUnity extends ComplexNumber {
	int nInternal;
	int kInternal;
    
	RootOfUnity(int n, int k) {
	    super(0,0); // don't use the internal fields of ComplexNumber.
	    this.nInternal = n;
	    this.kInternal = k;
	}
	/** override the real part to return cos(2*pi*k/n) **/
	public double getReal() {return Math.cos((2*Math.PI*nInternal)/kInternal);}
	/** override the imaginary part to return -sin(2*pi*k/n) **/
	public double getImaginary() {return -1*Math.sin((2*Math.PI*nInternal)/kInternal);}
    
	/* get k */
	public int getK() {return this.kInternal;}
	/* get n */
	public int getN() {return this.nInternal;}
    }
}
