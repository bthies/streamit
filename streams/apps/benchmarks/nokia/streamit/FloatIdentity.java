import streamit.*;
import streamit.io.*;

class FloatIdentity extends Filter
{
	public void init ()
	{
		setInput (Float.TYPE); setOutput (Float.TYPE);
		setPush (1); setPop (1);
	}
	public void work ()
	{
		output.pushFloat (input.popFloat ());
	}
}
