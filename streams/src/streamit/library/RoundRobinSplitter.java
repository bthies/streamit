package streamit;

public class RoundRobinSplitter extends Splitter
{
    public void Work ()
    {
        ASSERT (dest.size () > 0);
        
        PassOneData (input, output [outputIndex]);
        outputIndex = (outputIndex + 1) % dest.size ();
    }
}
