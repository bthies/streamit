/*
 * Outputs data to stdout
 */

import streamit.library.*;

class CharPrinter extends Filter
{
    public void init ()
    {
	input = new Channel (Character.TYPE, 1);     /* pops 1 [peek] */
    }
    public void work ()
    {
	System.out.println (input.popChar ());
    }
}
