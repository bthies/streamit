package at.dms.kjc.sir.linear;

/**
 * This class represents the cost (variously defined) of computing
 * the value of a linear filter representation. Eg it represents
 * the number of multiplies and adds that are necessary if we were to use
 * a direct implementation for one execution of a LinearFilterRepresentation.
 *<p>
 * Obviously, all of the multiplies and adds refer to floating point operations.
 **/
public class LinearCost {
    /** the number of multiplies. **/
    private int multiplyCount;
    /** the number of adds **/
    private int addCount;
    /** LinearCost with 0 multiplies and 0 adds. **/
    public static final LinearCost ZERO = new LinearCost(0,0);
    
    public LinearCost(int muls, int adds) {
	this.multiplyCount = muls;
	this.addCount = adds;
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
	return new LinearCost(this.getMultiplies() + other.getMultiplies(), // muls
			      this.getAdds() + other.getAdds()); // adds
    }

    
    private void checkRep() {
	if (this.multiplyCount < 0) {throw new RuntimeException("negative multiply count!");}
	if (this.addCount < 0) {throw new RuntimeException("negative add count!");}
    }

    /**
     * Returns the cost of this (in terms of estimated execution time,
     * relative to any metric) if implemented directly in the time
     * domain.
     */
    public int getDirectCost() {
	return 3 * multiplyCount + addCount;
    }

    /**
     * Returns the cost of this (in terms of estimated execution time,
     * relative to any metric) if implemented in the frequency domain.
     *
     * Must be comparable to values returned by getDirectCost().
     */
    public int getFrequencyCost() {
	return getDirectCost() / 2;
    }
    


}
	
	
