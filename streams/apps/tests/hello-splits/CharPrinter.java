import streamit.*;

public class CharPrinter extends Filter
{
    public Channel input = new Channel (Character.TYPE, 1);
    public void initIO ()
    {
        streamInput = input;
    }

    public void work()
    {
           System.out.print(streamInput.popChar());
    }

}

