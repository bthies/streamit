package streamit;

import java.util.ArrayList;
import java.util.Iterator;
import streamit.scheduler.SchedJoinType;
import streamit.scheduler.Scheduler;

public class NullJoiner extends Joiner
{
    public void work ()
    {
        // a null joiner should never have its work function called
        ERROR ("work function called in a NullJoiner");  
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    SchedJoinType getSchedType (Scheduler scheduler)
    {
        // the weights will just be an array of 0's
        ArrayList weights = new ArrayList (srcs.size ());

        {
            Iterator filterIter = srcs.iterator ();

            Integer zero = new Integer (0);

            int index;
            for (index = 0; index < srcs.size (); index++)
            {
                Stream filter = (Stream) filterIter.next ();
                
                ASSERT (filter);
                ASSERT (filter.getIOField ("output") == null);
                
                weights.add (zero);
            }
        }

        return scheduler.newSchedJoinType (SchedJoinType.NULL, weights, this);
    }
}
