package streamit.scheduler.simple;

import streamit.scheduler.SchedStream;
import streamit.scheduler.SchedPipeline;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.math.BigInteger;

public class SimpleSchedPipeline extends SchedPipeline implements SimpleSchedStream
{
    final SimpleHierarchicalScheduler scheduler;

    SimpleSchedPipeline (SimpleHierarchicalScheduler scheduler, Object stream)
    {
        super (stream);

        ASSERT (scheduler);
        this.scheduler = scheduler;
    }

    public Object computeSchedule ()
    {
        List mySchedule = new LinkedList ();

        List children = getChildren ();
        ListIterator iter = children.listIterator ();
        SchedStream prevChild = null;
        while (iter.hasNext ())
        {
            SchedStream child = (SchedStream) iter.next ();
            ASSERT (child);

            // can't quite handle peeking yet!
            ASSERT (child.getPeekConsumption () == child.getConsumption ());

            Object childSchedule;
            childSchedule = scheduler.computeSchedule (child);
            ASSERT (childSchedule);

            BigInteger numExecutions = child.getNumExecutions ();
            ASSERT (numExecutions != null && numExecutions.signum () == 1);

            // calculate the size of the buffer necessary
            if (prevChild != null)
            {
                // compute a simple size of the buffer
                BigInteger consumesSize = BigInteger.valueOf (child.getConsumption ());
                BigInteger bufferSize = consumesSize.multiply (numExecutions);

                // correct for possible peeking:
                if (child.getPeekConsumption () != child.getConsumption ())
                {
                    // first simply extend the buffer size by enough push amounts
                    // to accomodate the entire peek sequence
                    int extraPeekSize = child.getPeekConsumption () - child.getConsumption ();
                    int pushSize = child.getProduction ();

                    ASSERT (pushSize != 0);
                    int extraPushes = (extraPeekSize + pushSize - 1) / pushSize;

                    bufferSize = bufferSize.add (BigInteger.valueOf (extraPushes * pushSize));
                }

                scheduler.setBufferSize (prevChild.getStreamObject (), child.getStreamObject (), bufferSize);
            }

            // enter all the appropriate sub-schedules into the schedule
            while (numExecutions.signum () != 0)
            {
                mySchedule.add (childSchedule);
                numExecutions = numExecutions.subtract (BigInteger.ONE);
            }

            prevChild = child;
        }

        return mySchedule;
    }
}