/*
 * Outputs 8 integers to stdout at a time
 * and passes them through the pipe
 */

import streamit.library.*;

class BinaryPasser extends Filter
{
    String label;
    public BinaryPasser(String l) {
	this.label = l;
    }
    public void init ()
    {
	input  = new Channel (Integer.TYPE, 8);     /* pops 8 [peek] */
	output = new Channel (Integer.TYPE, 8);     /* pushes 8 */
    }
    public void work ()
    {
	int i;
	int[] data = new int[8];
	for (i=0; i<8; i++) {
	    data[i] = input.popInt();
	    output.pushInt(data[i]);
	}
	// output the label that corresponds to this passer
	System.out.print(this.label + ": ");
	// now, output the bits
	for (i=0; i<8; i++) {
	    System.out.print (data[8-i-1]);
	}
	System.out.println();
    }
}
