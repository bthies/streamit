/*
 * Strips out the zero bytes that are pushed into the stream
 */

import streamit.library.*;

class UnFrameMaker extends Filter
{
    int x;
    	
    public void init() {
	/* grabs framesize-1 data items from channel */
	input  = new Channel (Character.TYPE, FrameMaker.FRAMESIZE);
	/* pushes framesize data onto the channel */
	output = new Channel (Character.TYPE, FrameMaker.FRAMESIZE-1);
    }
    public void work ()
    {
	int i;
	// push the data bytes, ignore the last null ending byte
	for (i=0; i<FrameMaker.FRAMESIZE-1;i++) {
	    output.pushChar(input.popChar());
	}
	input.popChar();

    }
}


