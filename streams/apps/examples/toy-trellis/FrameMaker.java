/*
 * Creates frames of data for the trellis encoder/decoder to work
 */

import streamit.library.*;

class FrameMaker extends Filter
{
    int x;
    // frame size is size of data in frame plus one for terminating 0
    // (actually, we only need two bits of 0 to reset the encoder, but that is ok)
    static int FRAMESIZE = 5;
	
    public void init() {
	/* grabs framesize-1 data items from channel */
	input  = new Channel (Character.TYPE, FRAMESIZE-1);
	/* pushes framesize data onto the channel */
	output = new Channel (Character.TYPE, FRAMESIZE);
    }
    public void work ()
    {
	int i;
	// push all of the data, and then push a 0 to reset the encoder
	for (i=0; i<FRAMESIZE-1;i++) {
	    output.pushChar(input.popChar());
	}
	// throw on the terminating zero
	output.pushChar((char)0);
    }
}


