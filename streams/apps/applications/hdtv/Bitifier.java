/**
 * Bitifier -- converts a byte (masquarading as an integer)
 * into a stream of bits (also masquarading as integers).
 * In digital systems, this is known as a shift register
 * (MSB is shifted out first).
 **/
import streamit.library.*;

class Bitifier extends Filter {
    public void init() {
	input  = new Channel(Integer.TYPE, 1); // pops 1 "byte"
	output = new Channel(Integer.TYPE, 8); // pushes 8 "bits"
    }

    public void work() {
	int left = input.popInt(); // pull off the byte
	for (int i=0; i<8; i++) {
	    // shift out the bits one by one (msb first)
	    output.pushInt((left & 0x80) >> 7);
	    // set up next shift
	    left = left << 1;
	}
    }    
}
