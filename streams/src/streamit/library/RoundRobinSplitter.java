package streamit;

import streamit.scheduler.SchedSplitType;
import java.util.ArrayList;
import java.util.Iterator;

public class RoundRobinSplitter extends Splitter
{
    public void work ()
    {
        ASSERT (dest.size () > 0);
        ASSERT (output [outputIndex]);

        passOneData (input, output [outputIndex]);
        outputIndex = (outputIndex + 1) % dest.size ();
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    SchedSplitType getSchedType ()
    {
        ArrayList weights = new ArrayList (dest.size ());

        Integer one = new Integer (1);
        int index;
        for (index = 0; index < dest.size (); index++)
        {
            weights.add (one);
        }

        return new SchedSplitType (SchedSplitType.ROUND_ROBIN, weights);
    }
}
