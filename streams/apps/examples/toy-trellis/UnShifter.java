/**
 * Simple filter which converts a bitstream to chars
 * for input into the trellis encoder/decoder.
 * basically, we shift in 8 bits (least sig first)
 * as integers and output a char
 **/
import streamit.library.*;

class UnShifter extends Filter
{
    public void init() {
	input  = new Channel(Integer.TYPE, 8); // pops 8 bits to make a char
	output = new Channel(Character.TYPE, 1); // pushes 1 char out
    }

    public void work() {
	int i;
	int data;
	
	data = 0;
	
	for(i=0; i<8; i++) {
	    // add the current bit from the input stream
	    data = data | (input.popInt() << i);
	}
	output.pushChar((char)data);
    }
}

