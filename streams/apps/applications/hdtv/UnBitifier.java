/**
 * UnBitifier -- converts a stream of bits (masquarading as integers)
 * into byte (also masquarading as an integer).
 * In digital systems, this is also a shift register
 * (MSB is shifted in first).
 **/
import streamit.library.*;

class UnBitifier extends Filter {
    public void init() {
	input  = new Channel(Integer.TYPE, 8); // pops 1 "bits"
	output = new Channel(Integer.TYPE, 1); // pushes 1 "byte"
    }

    public void work() {
	int accum = 0;
	for (int i=0; i<8; i++) {
	    // shift in 8 bits
	    accum = accum << 1;
	    accum = accum | input.popInt();
	}
	output.pushInt(accum);
    }    
}
