package streamit;

import streamit.scheduler.SchedSplitType;
import streamit.scheduler.Scheduler;
import java.util.ArrayList;
import java.util.Iterator;

public class RoundRobinSplitter extends Splitter
{
    public void work ()
    {
        ASSERT (dest.size () > 0);
        ASSERT (streamOutput [outputIndex]);

        passOneData (streamInput, streamOutput [outputIndex]);
        outputIndex = (outputIndex + 1) % dest.size ();
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    SchedSplitType getSchedType (Scheduler scheduler)
    {
        ArrayList weights = new ArrayList (dest.size ());

        Integer one = new Integer (1);
        int index;
        for (index = 0; index < dest.size (); index++)
        {
            weights.add (one);
        }

        return scheduler.newSchedSplitType (SchedSplitType.ROUND_ROBIN, weights, this);
    }
}
