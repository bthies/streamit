import streamit.*;

public class CharPrinter extends Filter
{
    public void work()
    {
           System.out.print(input.popChar());
    }
    public void init ()
    {
        input = new Channel (Character.TYPE, 1);
    }

}

