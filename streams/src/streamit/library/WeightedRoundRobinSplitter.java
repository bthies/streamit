package streamit.library;

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
        int numChildren = dest.size ();
        int [] weights = new int [numChildren];
        
        int i;
        for (i=0;i<numChildren;i++)
        {
            if (dest.get(i) != null && ((Stream)dest.get (i)).input != null)
            {
                weights [i] = ((Integer)destWeight.get (i)).intValue ();
            }
        }
        
        return weights;
    }

    public int getConsumption ()
    {
        int numChildren = dest.size ();
        int inputTotal = 0;
        
        int i;
        for (i=0;i<numChildren;i++)
        {
            if (dest.get(i) != null && ((Stream)dest.get (i)).input != null)
            {
                inputTotal += ((Integer)destWeight.get (i)).intValue ();
            }
        }
        
        return inputTotal;
    }
}
