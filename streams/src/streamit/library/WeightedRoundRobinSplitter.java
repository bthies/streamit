package streamit;

import java.util.*;

public class WeightedRoundRobinSplitter extends Splitter
{
    List destWeight = new ArrayList ();
    int outputCount = 0;

    void AddWeight (Integer weight)
    {
        ASSERT (weight != null && weight.intValue () >= 0);
        destWeight.add (weight);
    }

    public boolean IsOutputUsed (int index)
    {
        ASSERT (index < destWeight.size ());
        return ((Integer)destWeight.get(index)).intValue () != 0;
    }

    public void ConnectGraph ()
    {
        // do I even have anything to do?
        ASSERT (dest.size () == destWeight.size ());

        super.ConnectGraph ();
    }

    public void Work ()
    {
        ASSERT (destWeight.size () == dest.size ());

        while (outputCount == ((Integer)destWeight.get (outputIndex)).intValue ())
        {
            outputCount = 0;
            outputIndex = (outputIndex + 1) % dest.size ();
        }

        PassOneData (input, output [outputIndex]);
        outputCount++;
    }
}
