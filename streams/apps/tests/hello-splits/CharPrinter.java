import streamit.*;

public class CharPrinter extends Filter 
{
    public void InitIO ()
    {
        input = new Channel(new char[1]);
    }

    public void Work()
    {
	   System.out.print(input.PopChar());
    }

}

