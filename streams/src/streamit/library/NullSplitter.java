package streamit;

import java.util.ArrayList;
import java.util.Iterator;
import streamit.scheduler.SchedSplitType;
import streamit.scheduler.Scheduler;

public class NullSplitter extends Splitter
{
    public void work ()
    {
        // a null splitter should never have its work function called
        ERROR ("work function called in a NullSplitter");  
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    SchedSplitType getSchedType (Scheduler scheduler)
    {
        // the weights will just be an array of 0's
        ArrayList weights = new ArrayList (dest.size ());

        {
            Iterator filterIter = dest.iterator ();

            Integer zero = new Integer (0);

            int index;
            for (index = 0; index < dest.size (); index++)
            {
                Stream filter = (Stream) filterIter.next ();
                
                ASSERT (filter);
                ASSERT (filter.getIOField ("input") == null);
                
                weights.add (zero);
            }
        }

        return scheduler.newSchedSplitType (SchedSplitType.NULL, weights, this);
    }
}
