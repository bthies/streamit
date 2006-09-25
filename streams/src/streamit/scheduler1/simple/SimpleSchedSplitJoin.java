package streamit.scheduler1.simple;

import streamit.scheduler1.SchedSplitJoin;
import streamit.scheduler1.SchedStream;
import streamit.scheduler1.simple.SimpleSchedStream;
import streamit.scheduler1.SchedRepSchedule;

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.math.BigInteger;

public class SimpleSchedSplitJoin extends SchedSplitJoin implements SimpleSchedStream
{
    final SimpleHierarchicalScheduler scheduler;
    private List<SchedRepSchedule> steadySchedule;
    private List<Object> initSchedule;
    int initDataConsumption = -1;

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
            steadySchedule = new LinkedList<SchedRepSchedule> ();
            initSchedule = new LinkedList<Object> ();
        }

        // compute the children's schedules and figure out
        // how many times the split needs to be executed to feed
        // all the buffers so the children can initialize (including the
        // peek - pop amounts!)
        int initSplitRunCount = 0;
        {
            List<SchedStream> children = getChildren ();
            ASSERT (children);

            // go through all the children and check how much
            int childNum = -1;
            ListIterator<SchedStream> iter = children.listIterator ();
            while (iter.hasNext ())
                {
                    // get the child
                    SimpleSchedStream child = (SimpleSchedStream) iter.next ();
                    ASSERT (child);
                    childNum ++;

                    // compute child's schedule
                    child.computeSchedule ();

                    // get the amount of data needed to initilize this child
                    int childInitDataConsumption = child.getInitDataConsumption ();

                    // add the amount of data needed to allow for peeking
                    // this is the total amount needed to intialize this path
                    // of the split join
                    childInitDataConsumption += (child.getPeekConsumption () - child.getConsumption ());

                    // now figure out how many times the split needs to be run in
                    // initialization to accomodate this child
                    int splitRunCount;
                    if (childInitDataConsumption != 0)
                        {
                            // just divide the amount of data needed by data received
                            // per iteration of the split
                            int splitDataSent = getSplitType ().getOutputWeight (childNum);
                            ASSERT (splitDataSent > 0);

                            splitRunCount = (childInitDataConsumption + splitDataSent - 1) / splitDataSent;
                        } else {
                            // the child doesn't need any data to intitialize, so I
                            // don't need to run the split for it at all
                            splitRunCount = 0;
                        }

                    // pick the max
                    if (splitRunCount > initSplitRunCount)
                        {
                            initSplitRunCount = splitRunCount;
                        }
                }
        }

        // compute the init schedule
        {
            // compute and save the amount of data consumed by
            // this split join on initialization
            initDataConsumption = initSplitRunCount * getSplitType ().getRoundConsumption ();

            // run through the split an appropriate number of times
            // and append it to the init schedule
            {
                Object splitObject = getSplitType ().getSplitObject ();
                ASSERT (splitObject);

                if (initSplitRunCount > 0)
                    {
                        initSchedule.add (new SchedRepSchedule (BigInteger.valueOf (initSplitRunCount), splitObject));
                    }
            }

            // now add the initialization schedules for all the children
            List<SchedStream> children = getChildren ();
            ASSERT (children);

            // go through all the children and check how much
            ListIterator<SchedStream> iter = children.listIterator ();
            while (iter.hasNext ())
                {
                    // get the child
                    SimpleSchedStream child = (SimpleSchedStream) iter.next ();
                    ASSERT (child);

                    // get child's init schedule and append it
                    if (child.getInitSchedule () != null)
                        {
                            initSchedule.add (child.getInitSchedule ());
                        }
                }
        }

        // compute the split schedule
        {
            Object splitObject = getSplitType ().getSplitObject ();
            ASSERT (splitObject);

            BigInteger numExecutions = getNumSplitExecutions ();
            ASSERT (numExecutions != null && numExecutions.signum () >= 0);

            if (numExecutions.signum () == 1)
                {
                    steadySchedule.add (new SchedRepSchedule (numExecutions, splitObject));
                }
        }

        // compute the schedule for the body of the splitjoin
        // and all the buffer schedule sizes
        {
            Object joinObject = getJoinType ().getJoinObject ();
            Object splitObject = getSplitType ().getSplitObject ();
            ASSERT (joinObject);
            ASSERT (splitObject);

            List<SchedStream> children = getChildren ();
            ASSERT (children);

            // go through all the children and add their schedules
            // to my schedule the appropriate number of times
            int nChild = -1;
            ListIterator<SchedStream> iter = children.listIterator ();
            while (iter.hasNext ())
                {
                    // get the child
                    SimpleSchedStream child = (SimpleSchedStream) iter.next ();
                    ASSERT (child);
                    nChild ++;

                    // get the child's schedule
                    Object childSchedule = child.getSteadySchedule ();
                    ASSERT (childSchedule);

                    BigInteger numExecutions = child.getNumExecutions ();
                    ASSERT (numExecutions != null && numExecutions.signum () == 1);

                    // compute buffer sizes between split and children and join
                    {
                        // the amount of data consumed/produced on every iteration of the schedule:
                        BigInteger inBuffer = numExecutions.multiply (BigInteger.valueOf (child.getConsumption ()));
                        BigInteger outBuffer = numExecutions.multiply (BigInteger.valueOf (child.getProduction ()));

                        // for the incoming schedule, add the extra left-over data after intialization
                        int splitInitData = getSplitType ().getOutputWeight (nChild) * initSplitRunCount;
                        inBuffer = inBuffer.add (BigInteger.valueOf (splitInitData - child.getInitDataConsumption ()));

                        // and make sure that we get the max of this amount and the amount needed to do initilization in the first place
                        inBuffer = inBuffer.max (BigInteger.valueOf (splitInitData));

                        // for the outgoing schedule, add the amount of data
                        // produced by the child on initialization
                        outBuffer = outBuffer.add (BigInteger.valueOf (child.getInitDataProduction ()));

                        // save these data
                        scheduler.schedule.setJoinBufferSize (splitObject, child.getStreamObject (), inBuffer);
                        scheduler.schedule.setSplitBufferSize (child.getStreamObject (), joinObject, outBuffer);
                    }

                    // in steady state schedule, every child should be executed
                    // at least once per iteration!
                    steadySchedule.add (new SchedRepSchedule (numExecutions, childSchedule));
                }
        }

        // compute the join schedule
        {
            Object joinObject = getJoinType ().getJoinObject ();
            ASSERT (joinObject);

            BigInteger numExecutions = getNumJoinExecutions ();
            ASSERT (numExecutions != null && numExecutions.signum () >= 0);

            if (numExecutions.signum () == 1)
                {
                    steadySchedule.add (new SchedRepSchedule (numExecutions, joinObject));
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

    public int getInitDataConsumption ()
    {
        ASSERT (initDataConsumption >= 0);
        return initDataConsumption;
    }

    public int getInitDataProduction ()
    {
        return 0;
    }
}

