/*
 * Simple Reed-Solomon Encoder/Decoder system working towards a
 * HDTV implementation.
 */

import streamit.library.*;

class ReedSolomonDemo extends StreamIt
{
    static public void main (String [] t)
    {
        ReedSolomonDemo test = new ReedSolomonDemo ();
        test.run (t);
    }

    public void init ()
    {
	add (new DataSource());
	add (new ReedSolomonEncoder());
	add (new ReedSolomonNoise());
	add (new CharPasser());
	add (new ReedSolomonDecoder());
	add (new CharPrinter());
    }
}



/*
 * Outputs data to stdout and passes it along the pipeline
 */

class CharPasser extends Filter
{
    public void init ()
    {
	input = new Channel (Character.TYPE, 1);     /* pops 1 [peek] */
	output = new Channel (Character.TYPE, 1);     /* pops 1 [peek] */
    }
    public void work ()
    {
	char t;

	t = input.popChar();
	System.out.print(t);
	output.pushChar(t);
	
    }
}



/*
 * Outputs data to stdout
 */

class CharPrinter extends Filter
{
    public void init ()
    {
	input = new Channel (Character.TYPE, 1);     /* pops 1 [peek] */
    }
    public void work ()
    {
	System.out.print (input.popChar ());
    }
}


