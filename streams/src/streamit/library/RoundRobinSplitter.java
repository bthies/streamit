package streamit;

public class RoundRobinSplitter extends Splitter
{
    public void Work ()
    {
        if (outputCount == ((Integer)destWeight.get (outputIndex)).intValue ())
        {
            outputCount = 0;
            outputIndex++;
        }
        
        if (outputIndex == dest.size ())
        {
            outputIndex = 0;
        }
        
        PassOneData (input, output [outputIndex]);
        outputCount++;
    }
}
