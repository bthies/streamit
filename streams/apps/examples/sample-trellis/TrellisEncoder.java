/*
 * Simple 1/2 trellis encoder
 * See http://www.mit.edu/~6.450/handouts.html Lecture 24
 * 
 */

import streamit.library.*;

class TrellisEncoder extends Filter
{
    // in Lecture 24, state0 corresponds to Dj-1
    // and state1 corresponds to Dj-2
    int state0, state1;

    public void init() {
	input =  new Channel (Integer.TYPE, 1);    /* pops 1 */
	output = new Channel (Integer.TYPE, 2);    /* pushes 2 */
	// start with initial state of (00)
	state0 = 0;
	state1 = 0;
    }
    public void work ()
    {
	int currentData;

	// grab the data from the input stream (eg Dj) 
	currentData = input.popInt();

	// push out Uj,1 (msb)
	output.pushInt(currentData ^ state0 ^ state1);
	// push out Uj,0 (lsb)
	output.pushInt(currentData ^ state1);

	// update the state
	state1 = state0;
	state0 = currentData;
    }
}


