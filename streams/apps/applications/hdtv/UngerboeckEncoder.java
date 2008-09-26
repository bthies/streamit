/*
 * Simple 1/2 Trellis encoder which uses an Ungerboeck Code
 * to add redundant information across bits.
 * 1/2 Trellis Encoder ("Optimal 4 state Ungerboeck code")
 *
 * x1 --------------> y1 ----------------------> z1
 *                                  |
 *                                  |
 *                                  v
 *                  (s0)     D---->XOR---->D---> z0
 *                           ^             | (s1)
 *                           |-------------
 * x2 is the first input bit, x1 is the second input bit.
 * output: z2,z1,z0 (to the symbol mapper)
 */

import streamit.library.*;

class UngerboeckEncoder extends Filter
{
    
    int state0, state1;

    public void init() {
	inputChannel=  new Channel (Integer.TYPE, 1);    /* pops 1 */
	outputChannel= new Channel (Integer.TYPE, 2);    /* pushes 2 */
	// start with initial state of (00)
	state0 = 0;
	state1 = 0;
    }
    public void work ()
    {

	// grab the data from the input stream (eg x1=y1) 
	int y1 = inputChannel.popInt();

	int z1 = y1;
	int z0 = state1;
	
	// push output
	outputChannel.pushInt(z1);
	outputChannel.pushInt(z0);

	// update the current state
	state1 = y1 ^ state0; // y1 XOR state0
	state0 = z0;
    }
}
