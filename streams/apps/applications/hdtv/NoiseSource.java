/**
 * Filter which produces parameterized noise in the
 * form of flipping a bit (eg this is stuck in the HDTV
 * encode pipeline before the symbol mapper.
 **/

import streamit.library.*;

class NoiseSource extends Filter {
    int currentCount; // keep track of what element we have seen
    int noisePeriod; // after how many elements we are supposed to be flipping sign 
    /** create a new noise source witht the period specified **/
    public NoiseSource(int period) {
	super(period);
    }
    public void init(int period) {
	this.noisePeriod = period;
	this.currentCount = 0;
	input = new Channel(Integer.TYPE, 1);
	output= new Channel(Integer.TYPE, 1);
    }
    public void work() {
	int t = input.popInt();
	// if we are ready to flip a bit...
	if (this.currentCount >= this.noisePeriod) {
	    //System.out.println("Changing bit");
	    t = t ^ 1;
	    this.currentCount = 0;
	}
	output.pushInt(t);
	this.currentCount++;
    }
}
