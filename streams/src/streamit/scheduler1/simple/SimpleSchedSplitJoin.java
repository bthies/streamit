package streamit.scheduler.simple;

import streamit.scheduler.SchedSplitJoin;
import streamit.scheduler.SchedStream;
import streamit.scheduler.simple.SimpleSchedStream;

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.math.BigInteger;

public class SimpleSchedSplitJoin extends SchedSplitJoin implements SimpleSchedStream
{
    final SimpleHierarchicalScheduler scheduler;
    private List steadySchedule;
    private List initSchedule;

    SimpleSchedSplitJoin (SimpleHierarchicalScheduler scheduler, Object stream)
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

        // compute the split schedule and the split buffer sizes
        {
            Object splitObject = getSplitType ().getSplitObject ();
            ASSERT (splitObject);

            BigInteger numExecutions = getNumSplitExecutions ();
            ASSERT (numExecutions != null && numExecutions.signum () == 1);

            while (!numExecutions.equals (numExecutions.ZERO))
            {
                numExecutions = numExecutions.subtract (numExecutions.ONE);

                steadySchedule.add (splitObject);
            }
        }

        // compute the schedule for the body of the splitjoin
        {
            Object joinObject = getJoinType ().getJoinObject ();
            Object splitObject = getSplitType ().getSplitObject ();
            ASSERT (joinObject);
            ASSERT (splitObject);

            List children = getChildren ();
            ASSERT (children);

            // go through all the children and add their schedules
            // to my schedule the appropriate number of times
            ListIterator iter = children.listIterator ();
            while (iter.hasNext ())
            {
                // get the child
                SimpleSchedStream child = (SimpleSchedStream) iter.next ();
                ASSERT (child);

                // compute child's schedule
                child.computeSchedule ();
                Object childSchedule = child.getSteadySchedule ();
                ASSERT (childSchedule);

                BigInteger numExecutions = child.getNumExecutions ();
                ASSERT (numExecutions != null && numExecutions.signum () == 1);

                // compute buffer sizes between split and children and join
                BigInteger inBuffer = numExecutions.multiply (BigInteger.valueOf (child.getConsumption ()));
                BigInteger outBuffer = numExecutions.multiply (BigInteger.valueOf (child.getProduction ()));

                scheduler.schedule.setJoinBufferSize (splitObject, child.getStreamObject (), inBuffer);
                scheduler.schedule.setSplitBufferSize (child.getStreamObject (), joinObject, outBuffer);

                // add the schedule numExecutions times
                while (!numExecutions.equals (numExecutions.ZERO))
                {
                    numExecutions = numExecutions.subtract (numExecutions.ONE);

                    steadySchedule.add (childSchedule);
                }
            }
        }

        // compute the join schedule and the join buffer sizes
        {
            Object joinObject = getJoinType ().getJoinObject ();
            ASSERT (joinObject);

            BigInteger numExecutions = getNumJoinExecutions ();
            ASSERT (numExecutions != null && numExecutions.signum () == 1);

            while (!numExecutions.equals (numExecutions.ZERO))
            {
                numExecutions = numExecutions.subtract (numExecutions.ONE);

                steadySchedule.add (joinObject);
            }
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

    public int getInitDataCount ()
    {
        ASSERT (false);
        return 0;
    }
}

