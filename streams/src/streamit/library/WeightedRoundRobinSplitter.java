package streamit;

import streamit.scheduler.SchedSplitType;
import streamit.scheduler.Scheduler;
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

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    SchedSplitType getSchedType (Scheduler scheduler)
    {
        return scheduler.newSchedSplitType (SchedSplitType.WEIGHTED_ROUND_ROBIN, destWeight, this);
    }
}
