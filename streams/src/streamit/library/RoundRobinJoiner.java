package streamit;

import streamit.scheduler.SchedJoinType;
import streamit.scheduler.Scheduler;
import java.util.ArrayList;
import java.util.Iterator;

public class RoundRobinJoiner extends Joiner 
{
    int weight;

    RoundRobinJoiner (int weight)
    {
        this.weight = weight;
    }

    public void work ()
    {
        int inputIndex;
        for (inputIndex = 0; inputIndex < srcs.size (); inputIndex++)
        {
            ASSERT (input [inputIndex]);
            int w;
            for (w = 0; w < weight; w++)
            {
                passOneData (input [inputIndex], output);
            }
        }
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    SchedJoinType getSchedType (Scheduler scheduler)
    {
        ArrayList weights = new ArrayList (srcs.size ());

        Integer w = new Integer (weight);
        int index;
        for (index = 0; index < srcs.size (); index++)
        {
            weights.add (w);
        }

        return scheduler.newSchedJoinType (SchedJoinType.WEIGHTED_ROUND_ROBIN, weights, this);
    }
}
