package at.dms.kjc.sir.statespace;

import at.dms.util.Utils;
import at.dms.kjc.sir.lowering.partition.linear.LinearPartitioner;

/**
 * This class represents the cost (variously defined) of computing
 * the value of a linear filter representation. I.e. it represents
 * the number of multiplies and adds that are necessary if we were to use
 * a direct implementation for one execution of a LinearFilterRepresentation.<br>
 *
 * Obviously, all of the multiplies and adds refer to floating point operations.int cols
 **/
public class LinearCost {
    /**
     * the factor by which we scale up all our costs -- work with
     * integers instead of floats so we can do quick and accurate
     * comparison in traceback.
     */
    private static final long SCALE_FACTOR = 100000;
    /** the number of multiplies. **/
    private int multiplyCount;
    /** the number of adds **/
    private int addCount;
    /** the number of rows (push count) in the matrix from which this was derived **/
    private int rows;
    /** the number of columns (pop count) in the matrix from which this was derived **/
    private int cols;
    /** LinearCost with 0 multiplies and 0 adds. **/
    public static final LinearCost ZERO = new LinearCost(0,0,0,0);
    
    /**
     * Note that muls and adds do NOT count
     * multiplication/addition by zero or multiplication by one,
     * whereas originalMatrixSize
     * gives the number of elements (including zero and one) that were
     * in the original matrix.
     */
    public LinearCost(int muls, int adds, int rows, int cols) {
	this.multiplyCount = muls;
	this.addCount = adds;
	this.rows = rows;
	this.cols = cols;
	checkRep();
    }

    /* Get the number of multiplications for this LinearCost. **/
    public int getMultiplies() {return this.multiplyCount;}
    /* Get the number of additions for this LinearCost. **/
    public int getAdds()       {return this.addCount;}

    /** Returns true if this represents less computation than other. **/
    public boolean lessThan(LinearCost other) {
	this.checkRep();
	other.checkRep();
	
	// use a simple sum of the number of operations for now
	int thisSum  = this.getMultiplies()  + this.getAdds();
	int otherSum = other.getMultiplies() + other.getAdds();	

	// if they have the same sum, choose based on multiplies
	if(thisSum == otherSum)
	    return (this.getMultiplies() < other.getMultiplies());

	return (thisSum < otherSum);
    }

    /** returns a new LinearCost that represents the sum (element wise) of this and other. **/
    public LinearCost plus(LinearCost other) {
	return new LinearCost(this.getMultiplies() + other.getMultiplies(), // muls
			      this.getAdds() + other.getAdds(),
			      this.rows,
			      this.cols);
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
     * some time to execute).<br>
     *
     * We scale up by FREQ_BENEFIT here instead of dividing in
     * getFrequencyCost so that eveverything stays integral.
     */
    public long getDirectCost() {
	// add the push count now to estimate copying overhead, even
	// if you're not adding/multiplying.
	return SCALE_FACTOR * (185l + 2l*cols + (3l*(long)multiplyCount) + ((long)addCount));
    }

}
