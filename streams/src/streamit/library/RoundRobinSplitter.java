package streamit;

import streamit.scheduler.SchedSplitType;
import streamit.scheduler.Scheduler;
import java.util.ArrayList;
import java.util.Iterator;

public class RoundRobinSplitter extends Splitter
{
    int weight;
    RoundRobinSplitter (int weight)
    {
        this.weight = weight;
    }
    public void work ()
    {
        int outputIndex;
        for (outputIndex = 0; outputIndex < dest.size (); outputIndex++)
        {
            int w;
            for (w = 0; w < weight; w++)
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
        ArrayList weights = new ArrayList (dest.size ());

        Integer w = new Integer (weight);
        int index;
        for (index = 0; index < dest.size (); index++)
        {
            weights.add (w);
        }

        return scheduler.newSchedSplitType (SchedSplitType.WEIGHTED_ROUND_ROBIN, weights, this);
    }
}
