/*
 * Outputs data to stdout
 */

import streamit.library.*;

class IntPrinter extends Filter
{
    public void init ()
    {
	input = new Channel (Integer.TYPE, 1);     /* pops 1 [peek] */
    }
    public void work ()
    {
	int temp;
	temp = input.popInt();
	System.out.println (temp);
    }
}
