import streamit.library.*;

public class CharPrinter extends Filter
{
    public void work()
    {
           System.out.println(input.popChar());
    }
    public void init ()
    {
        input = new Channel (Character.TYPE, 1);
    }

}

