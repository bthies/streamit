package streamit;

public class RoundRobinSplitter extends Splitter
{
    public void Work ()
    {
        while (outputCount == ((Integer)destWeight.get (outputIndex)).intValue ())
        {
            outputCount = 0;
            outputIndex = (outputIndex + 1) % dest.size ();
        }
        
        PassOneData (input, output [outputIndex]);
        outputCount++;
    }
}
