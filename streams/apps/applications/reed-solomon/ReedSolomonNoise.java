/**
 * (Controllable) noise source for the Reed-Solomon
 * encoding/decoding demo program
 *
 * Andrew Lamb aalamb@mit.edu 6/17/2002
 * $Id: ReedSolomonNoise.java,v 1.3 2003-09-29 09:06:13 thies Exp $
 **/
import streamit.library.*;

class ReedSolomonNoise extends Filter {
    // locations of the noise to be added, NOISELOC[i] =n means
    // that the nth item poped from the input tape
    // will be corrupted by some noise. Must be strictly _increasing_
    // indicies
    int[] NOISELOC;
    int current_noise;
    int x;
    
    public void init() {
	input  = new Channel(Character.TYPE, 1); // pops 1
	output = new Channel(Character.TYPE, 1); // pushes 1 (possibly with an error)
	// set up state for the noise adder
	initNoise();
    }

    public void work() {
	int in;
 	int out;
	// if this is a noise location
	if (x == NOISELOC[current_noise]) {
	    in = (int)input.popChar();
	    out = in ^ 0xFF; // just xor with 11111111 (eg flip all bits)
	    output.pushChar((char)out);
	    current_noise++; // look for the next noise location
	} else {
	    output.pushChar(input.popChar());
	}
	this.x++;
	
	//this.x = (this.x + 1) % FIRSTNOISE;
    }

    /** Initializes the location (eg which byte of data) noise will be introduced into. **/
    public void initNoise() {
	/** set up the noise locations **/
	NOISELOC = new int[10];
	// put non increasing entry to end noise
	NOISELOC[0] = 5;
	NOISELOC[1] = 22;
	NOISELOC[2] = 23;
	NOISELOC[3] = 39;
	NOISELOC[4] = 0;
	NOISELOC[5] = 0;
	NOISELOC[6] = 0;
	NOISELOC[7] = 0;
	NOISELOC[8] = 0;
	NOISELOC[9] = 0;
	
	this.current_noise = 0;
	this.x = 0;
    }
}    

    
