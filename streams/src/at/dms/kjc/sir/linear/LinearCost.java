package at.dms.kjc.sir.linear;

import at.dms.util.Utils;
import at.dms.kjc.sir.lowering.partition.linear.LinearPartitioner;
import at.dms.kjc.sir.linear.frequency.*;

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
    /** the number of pops in the filter **/
    private int popCount;
    /** the number of rows (peek count) in the matrix from which this was derived **/
    private int rows;
    /** the number of columns (push count) in the matrix from which this was derived **/
    private int cols;
    /** LinearCost with 0 multiplies and 0 adds. **/
    public static final LinearCost ZERO = new LinearCost(0,0,0,0,0);
    
    /**
     * Note that muls and adds do NOT count
     * multiplication/addition by zero or multiplication by one,
     * whereas originalMatrixSize
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
	return (thisSum < otherSum);
    }

    /** returns a new LinearCost that represents the sum (element wise) of this and other. **/
    public LinearCost plus(LinearCost other) {
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

    /**
     * Returns the cost of this (in terms of estimated execution time,
     * relative to any metric) if implemented in the frequency domain.<br>
     *
     * Must be comparable to values returned by getDirectCost().
     */
    public long getFrequencyCost() {
	// okay.  this works by taking the fft cost, adding 1 as a
	// constant overhead per node, adding a small percentage of
	// the rows cost to indicate copying overhead (if you don't
	// divide by 185, then this completely dominates and will
	// distort the partitioning), then multiply by popcount since
	// the node has to be repeated, redundantly, if some of its
	// outputs are useless.
	double nodeCost = (((double)SCALE_FACTOR) *
			   (getFrequencyComputationCost() +
			    185.0 +
			    ((float)(2*cols))) *
			   ((double)Math.max(popCount,1)));
	// just count the pushing since popping is almost free, all at
	// once.  count it twice since you have to read it and write
	// it.
	long decimatorCost = SCALE_FACTOR * (popCount > 1 ? 185 + 4 * cols : 0);
	if (LinearPartitioner.DEBUG) {
	    System.err.println("Returning linear cost of " + ((long)nodeCost + decimatorCost) + " with: \n"
			       + "  frequencyComputationCost=" + getFrequencyComputationCost() + "\n"
			       + "  nodeCost=" + nodeCost + "\n"
			       + "  decimatorCost=" + decimatorCost + "\n"
			       + "  rows=" + rows + "\n"
			       + "  cols=" + cols + "\n"
			       + "  popCount=" + popCount); }
	return (long)nodeCost + decimatorCost;
    }

    /**
     * Gives an estimate of the cost of the actual FFT operation.<br>
     *
     * This is based on a regression of execution time for a program
     * in frequency and in time for varying sizes (taps) of FIR's.
     * The regression result is:<br>
     *
     * time_in_freq(taps) = 0.65 + ln(1+ (time_in_time(taps)-time_in_time(0)) / (1 + taps/50))<br>
     *
     * The offset 0.65 was obtained experimentally.
     */
    public double getFrequencyComputationCost() {
	return (((double)cols) *
		Math.log(1.0 + ((float)(4*rows)) /
			 (1.0 + ((float)LEETFrequencyReplacer.calculateN(rows))/50.0)));
    }
}
