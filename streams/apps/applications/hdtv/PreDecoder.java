/**
 * Simple decoder for the precoder used in
 * the HDTV system.
 * given y(t) we need to be able to find x(t) and state(t+1)
 *   x(t) = y(t) XOR state(t)
 *   state(t+1) = y(t)
 *
 * y(t) ----------> XOR -----> x(t)
 *          |        ^
 *          |        |
 *          ---->D---| (state)
 *
 * This filter, as with the others in the encoding process,
 * takes integers as inputs, but treats them as though they were bits.
 **/
import streamit.library.*;

class PreDecoder extends Filter {
    int state = 0; // start with state 0
    public void init() {
	input = new Channel(Integer.TYPE, 1);
	output= new Channel(Integer.TYPE, 1);
    }
    public void work() {
	int y = input.popInt();

	// calculate the input of the precoder given that it 
	// produced y as an output
	int x = y ^ this.state;

	// update our state
	this.state = y;

	// push out the output
	output.pushInt(x);
    }
}
