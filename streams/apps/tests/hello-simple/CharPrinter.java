import streamit.*;

public class CharPrinter extends Filter
{
    public void InitIO ()
    {
        input = new Channel(Character.TYPE);
    }

    public void Work()
    {
	   System.out.print(input.PopChar());
    }

}

