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
    private List steadySchedule = null;
    private List initSchedule = null;

    SimpleSchedPipeline (SimpleHierarchicalScheduler scheduler, Object stream)
    {
        super (stream);

        ASSERT (scheduler);
        this.scheduler = scheduler;
    }

    public void computeSchedule ()
    {
        // make sure that this the first call to computeSchedule
        {
            ASSERT (steadySchedule == null && initSchedule == null);
            steadySchedule = new LinkedList ();
            initSchedule = new LinkedList ();
        }

        List children = getChildren ();
        ListIterator iter = children.listIterator ();
        SimpleSchedStream prevChild = null;

        while (iter.hasNext ())
        {
            // get the child
            SimpleSchedStream child = (SimpleSchedStream) iter.next ();
            ASSERT (child);

            // initialize the child's schedule
            child.computeSchedule ();

            // can't quite handle peeking yet!
            ASSERT (child.getPeekConsumption () == child.getConsumption ());

            Object childSchedule;
            childSchedule = child.getSteadySchedule ();
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
                    int extraPushes = (extraPeekSize + (pushSize - 1)) / pushSize;

                    bufferSize = bufferSize.add (BigInteger.valueOf (extraPushes * pushSize));
                }

                scheduler.schedule.setBufferSize (prevChild.getStreamObject (), child.getStreamObject (), bufferSize);
            }

            // enter all the appropriate sub-schedules into the schedule
            while (numExecutions.signum () != 0)
            {
                steadySchedule.add (childSchedule);
                numExecutions = numExecutions.subtract (BigInteger.ONE);
            }

            prevChild = child;
        }
    }

    public Object getSteadySchedule ()
    {
        ASSERT (steadySchedule);
        return steadySchedule;
    }
    public Object getInitSchedule ()
    {
        ASSERT (initSchedule);
        return initSchedule;
    }
}