package streamit;

import java.util.ArrayList;
import java.util.Iterator;
import streamit.scheduler.SchedSplitType;
import streamit.scheduler.Scheduler;

public class DuplicateSplitter extends Splitter
{
    public void work ()
    {
        duplicateOneData (streamInput, streamOutput);
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    SchedSplitType getSchedType (Scheduler scheduler)
    {
        ArrayList weights = new ArrayList (dest.size ());

        {
            Iterator filterIter = dest.iterator ();

            Integer one = new Integer (1);
            Integer zero = new Integer (0);

            int index;
            for (index = 0; index < dest.size (); index++)
            {
                Stream filter = (Stream) filterIter.next ();
                ASSERT (filter);

                if (filter.getIOField ("streamInput") != null) weights.add (one);
                else weights.add (zero);
            }
        }

        return scheduler.newSchedSplitType (SchedSplitType.DUPLICATE, weights, this);
    }
}
