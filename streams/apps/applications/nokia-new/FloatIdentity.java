import streamit.*;
import streamit.io.*;

class FloatIdentity extends Filter
{
	public void init ()
	{
	    input = new Channel(Float.TYPE, 1);
	    output = new Channel(Float.TYPE, 1);
	}
	public void work ()
	{
		output.pushFloat (input.popFloat ());
	}
}
