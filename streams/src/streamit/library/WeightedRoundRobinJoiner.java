package streamit;

import streamit.scheduler.SchedJoinType;
import streamit.scheduler.Scheduler;
import java.util.*;

public class WeightedRoundRobinJoiner extends Joiner {
    List srcsWeight = new ArrayList ();

    void addWeight (Integer weight)
    {
        ASSERT (weight != null && weight.intValue () >= 0);

        srcsWeight.add (weight);
    }

    public boolean isInputUsed (int index)
    {
        ASSERT (index < srcsWeight.size ());
        return ((Integer)srcsWeight.get(index)).intValue () != 0;
    }

    public void connectGraph ()
    {
        // do I even have anything to do?
        ASSERT (srcs.size () == srcsWeight.size ());
        super.connectGraph ();
    }

    public void work ()
    {
        ASSERT (srcsWeight.size () == srcs.size ());

        int inputIndex;
        for (inputIndex = 0; inputIndex < srcs.size (); inputIndex++)
        {
            int inputCount;
            for (inputCount = ((Integer)srcsWeight.get (inputIndex)).intValue (); inputCount > 0 ; inputCount--)
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
        return scheduler.newSchedJoinType (SchedJoinType.WEIGHTED_ROUND_ROBIN, srcsWeight, this);
    }
}
