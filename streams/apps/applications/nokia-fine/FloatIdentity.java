import streamit.library.*;
import streamit.library.io.*;

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
	    //input.popFloat ();
	    //output.pushFloat(1f);
	}
}
