package streamit;

import streamit.scheduler.SchedJoinType;
import java.util.*;

public class WeightedRoundRobinJoiner extends Joiner {
    List srcsWeight = new ArrayList ();
    int inputCount = 0;

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
        while (inputCount == ((Integer)srcsWeight.get (inputIndex)).intValue ())
        {
            inputCount = 0;
            inputIndex = (inputIndex + 1) % srcs.size ();
        }

        passOneData (streamInput [inputIndex], streamOutput);
        inputCount++;
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    SchedJoinType getSchedType ()
    {
        return new SchedJoinType (SchedJoinType.WEIGHTED_ROUND_ROBIN, srcsWeight, this);
    }
}
