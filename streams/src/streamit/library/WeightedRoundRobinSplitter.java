package streamit;

import streamit.scheduler.SchedSplitType;
import java.util.*;

public class WeightedRoundRobinSplitter extends Splitter
{
    List destWeight = new ArrayList ();
    int outputCount = 0;

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

        while (outputCount == ((Integer)destWeight.get (outputIndex)).intValue ())
        {
            outputCount = 0;
            outputIndex = (outputIndex + 1) % dest.size ();
        }

        passOneData (streamInput, streamOutput [outputIndex]);
        outputCount++;
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    SchedSplitType getSchedType ()
    {
        return new SchedSplitType (SchedSplitType.WEIGHTED_ROUND_ROBIN, destWeight, this);
    }
}
