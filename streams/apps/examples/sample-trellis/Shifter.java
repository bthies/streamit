/**
 * Simple filter which converts chars to a bit stream
 * for input into the trellis encoder/decoder.
 * basically, we shift out the bits (least sig first)
 * as integers into the output stream
 **/
import streamit.library.*;

class Shifter extends Filter
{
    public void init() {
	input  = new Channel (Character.TYPE, 1); /* pops 1 char */
	output = new Channel (Integer.TYPE, 8);    /* pushes 8 ints */
    }
    public void work ()
    {
	char currentChar = input.popChar();
	byte data = (byte)currentChar;
	int i;

	for (i=0; i<8; i++) {
	    // push out the current lsb
	    output.pushInt(data & 0x1);
	    // do a right shift
	    data = (byte)(data >> 1);
	}
    }
}
