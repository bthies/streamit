package at.dms.kjc.sir.linear;

import java.util.*;

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

    public LinearCost(int muls, int adds) {
	this.multiplyCount = muls;
	this.addCount = adds;
	checkRep();
    }

    public int getMultiplies() {return this.multiplyCount;}
    public int getAdds()       {return this.addCount;}
    
    private void checkRep() {
	if (this.multiplyCount < 0) {throw new RuntimeException("negative multiply count!");}
	if (this.addCount < 0) {throw new RuntimeException("negative add count!");}
    }
}
	
	
