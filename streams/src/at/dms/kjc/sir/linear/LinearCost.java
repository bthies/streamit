package at.dms.kjc.sir.linear;

import at.dms.util.Utils;

/**
 * This class represents the cost (variously defined) of computing
 * the value of a linear filter representation. Eg it represents
 * the number of multiplies and adds that are necessary if we were to use
 * a direct implementation for one execution of a LinearFilterRepresentation.
 *<p>
 * Obviously, all of the multiplies and adds refer to floating point operations.int cols
 **/
public class LinearCost {
    /** the factor by which operations are more expensive in time than
     * frequency (from empirical observations) **/
    private static final int FREQUENCY_BENEFIT = 50;
    /** the number of multiplies. **/
    private int multiplyCount;
    /** the number of adds **/
    private int addCount;
    /** the number of pops in the filter **/
    private int popCount;
    /** the number of rows (peek count) in the matrix from which this was derived **/
    private int rows;
    /** the number of columns (push count) in the matrix from which this was derived **/
    private int cols;
    /** LinearCost with 0 multiplies and 0 adds. **/
    public static final LinearCost ZERO = new LinearCost(0,0,0,0,0);
    
    /**
     * Note that <muls> and <adds> do NOT count
     * multiplication/addition by zero, whereas <originalMatrixSize>
     * gives the number of elements (including zero and one) that were
     * in the original matrix.
     */
    public LinearCost(int muls, int adds, int rows, int cols, int popCount) {
	this.multiplyCount = muls;
	this.addCount = adds;
	this.rows = rows;
	this.cols = cols;
	this.popCount = popCount;
	checkRep();
    }

    public int getMultiplies() {return this.multiplyCount;}
    public int getAdds()       {return this.addCount;}

    /** returns true if this represents less computation than other. **/
    public boolean lessThan(LinearCost other) {
	this.checkRep();
	other.checkRep();
	// use a simple sum of the number of operations for now
	int thisSum  = this.getMultiplies()  + this.getAdds();
	int otherSum = other.getMultiplies() + other.getAdds();
	return (thisSum < otherSum);
    }

    /** returns a new LinearCost that represents the sum (element wise) of this and other. **/
    public LinearCost plus(LinearCost other) {
	//Utils.fail("LinearCost.plus is deprecated -- doesn't make sense now that rows, cols, popCount included.  Remove this and all that depends on it after PLDI final copy.");
	return new LinearCost(this.getMultiplies() + other.getMultiplies(), // muls
			      this.getAdds() + other.getAdds(),
			      this.rows,
			      this.cols,
			      this.popCount);
    }
    
    private void checkRep() {
	if (this.multiplyCount < 0) {throw new RuntimeException("negative multiply count!");}
	if (this.addCount < 0) {throw new RuntimeException("negative add count!");}
    }

    /**
     * Returns the cost of this (in units proportional of estimated
     * execution time, relative to any metric) if implemented directly
     * in the time domain.  It's important to count the adds because
     * we don't currently count the multiplies if it's multiplication
     * by one!  Also add one so that no linear node is completely free
     * (even if it just does reordering or rate-changing, it takes
     * some time to execute).
     *
     * We scale up by FREQ_BENEFIT here instead of dividing in
     * getFrequencyCost so that eveverything stays integral.
     */
    public int getDirectCost() {
	return 1 + FREQUENCY_BENEFIT * (3 * multiplyCount + addCount);
    }

    /**
     * Returns the cost of this (in terms of estimated execution time,
     * relative to any metric) if implemented in the frequency domain.
     *
     * Must be comparable to values returned by getDirectCost().
     */
    public int getFrequencyCost() {
	// Multiply by factor of 4 because above we count multilies 3
	// times and adds once.  Even though we only add N-1 times for
	// a column of N, we add again for the constant vector, so
	// it's not off by one.  Then add the rows to represent the
	// overhead of copying input items, and of doing the FFT (it
	// might actually be cols*log(cols) or something, but
	// disregard this.)  Finally, multiply by the pop count since
	// we have to duplicate the whole effort for every item that
	// we pop.
	return (4 * rows * cols + rows) * popCount;
    }
}
