/**
 * Adds noise to a signal by flipping bits. The filter
 * expects that all of the input integers are either
 * zeros or ones.
 **/

import streamit.library.*;

class NoiseSource extends Filter {
    int flipTimes;
    int currentCount;

    
    /**
     * create a noise source that adds noise less than
     * randmly -- it flips a bit every flip times
     **/
    public NoiseSource(int flip) {
	this.flipTimes = flip;
    }

	
    public void init() {
	input = new Channel(Integer.TYPE, 1);
	output = new Channel(Integer.TYPE, 1);
	currentCount = 0;
    }

    public void work() {
	if (currentCount == (flipTimes-1)) {
	    output.pushInt(flipBit(input.popInt()));
	    System.out.println("flipped bit.");
	} else {
	    output.pushInt(input.popInt());
	}
	currentCount = (currentCount + 1) % flipTimes;
    }

    public int flipBit(int bit) {
	if (bit == 0) {
	    return 1;
	} else {
	    return 0;
	}
    }
}
