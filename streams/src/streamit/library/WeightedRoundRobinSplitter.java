package streamit;

import java.util.*;

public class WeightedRoundRobinSplitter extends Splitter
{
    List destWeight = new ArrayList ();

    void addWeight (Integer weight)
    {
        ASSERT (weight != null && weight.intValue () >= 0);
        destWeight.add (weight);
    }

    public boolean isOutputUsed (int index)
    {
        ASSERT (index < destWeight.size ());
        return ((Integer)destWeight.get(index)).intValue () != 0;
    }

    public void connectGraph ()
    {
        // do I even have anything to do?
        ASSERT (dest.size () == destWeight.size ());

        super.connectGraph ();
    }

    public void work ()
    {
        ASSERT (destWeight.size () == dest.size ());

        int outputIndex;
        for (outputIndex = 0; outputIndex < dest.size (); outputIndex++)
        {
            int outputCount;
            for (outputCount = ((Integer)destWeight.get (outputIndex)).intValue (); outputCount > 0 ; outputCount--)
            {
                passOneData (input, output [outputIndex]);
            }
        }
    }

    public int [] getWeights ()
    {
        // not debugged yet
        ASSERT (0);
        int numChildren = dest.size ();
        int [] weights = new int [numChildren + 1];
        int sumInput = 0;
        
        for (int i = 0; i < numChildren; i++)
        {
            weights [i+1] = ((Integer)destWeight.get (i)).intValue ();
            sumInput += weights [i+1];
        }
        
        weights [0] = sumInput;
        return weights;
    }
}
