package streamit.scheduler.simple;

import streamit.scheduler.SchedStream;
import streamit.scheduler.SchedPipeline;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;
import java.math.BigInteger;

public class SimpleSchedPipeline extends SchedPipeline implements SimpleSchedStream
{
    final SimpleHierarchicalScheduler scheduler;
    private List steadySchedule = null;
    private List initSchedule = null;
    private int initDataCount = 0;

    SimpleSchedPipeline (SimpleHierarchicalScheduler scheduler, Object stream)
    {
        super (stream);

        ASSERT (scheduler);
        this.scheduler = scheduler;
    }

    public void computeSchedule ()
    {
        // make sure that this the first (and thus only) call to computeSchedule
        {
            ASSERT (steadySchedule == null && initSchedule == null);
            steadySchedule = new LinkedList ();
            initSchedule = new LinkedList ();
        }

        List children = getChildren ();

        // first go through all children and compute their schedules
        // this will have the effect of computing the initialization
        // schedules for all the children - after we'll be able to use
        // the initDataCount to figure out how many data each child needs
        // for its own initialization, and thus how many data we need to feed
        // the child.
        {
            ListIterator iter = children.listIterator ();
            while (iter.hasNext ())
            {
                SimpleSchedStream child = (SimpleSchedStream) iter.next ();
                child.computeSchedule ();
            }
        }

        // now go through all the children again and figure out how many
        // data each child needs to be executed to fill up the buffers
        // enough to initialize the entire pipeline without producing
        // any data - this means that the last child doesn't get executed
        // even once
        // I do this by iterating from end to beggining and computing the
        // number of elements that each stream needs to produce
        Map numExecutionsForInit = new HashMap ();
        {
            int consumedByPrev = 0;

            // this is silly - I need to iterate from end to beginning:
            ListIterator iter = children.listIterator (children.size ());

            while (iter.hasPrevious ())
            {
                // get the child
                SimpleSchedStream child = (SimpleSchedStream) iter.previous ();
                ASSERT (child);

                // now figure out how many times this child needs to be run
                int producesPerIter = child.getProduction ();
                int numItersInit;
                if (producesPerIter != 0)
                {
                    // this stream actually produces some data - this is
                    // the common case
                    numItersInit = (consumedByPrev + producesPerIter - 1) / producesPerIter;
                } else {
                    // this stream does not produce any data
                    // make sure that consumedByPrev is 0 (otherwise
                    // I cannot execute the next child, 'cause it will
                    // never get any input)
                    ASSERT (consumedByPrev == 0);

                    // there will be no cycles executed for initialization
                    // of children downstream
                    numItersInit = 0;
                }

                // associate the child with the number of executions required
                // by the child to fill up the pipeline
                numExecutionsForInit.put (child, new Integer (numItersInit));

                // and figure out how many data this particular child
                // needs to initialize the pipeline
                consumedByPrev = numItersInit * child.getConsumption () +
                                 (child.getPeekConsumption () - child.getConsumption ()) +
                                 child.getInitDataCount ();
            }
        }

        // compute an initialization schedule
        // now that I know how many times each child needs to be executed,
        // it should be easy to compute this - just execute each stream
        // an appropriate number of times.
        // In this approach, I do not care about the size of the buffers, and
        // may in fact be enlarging them much more than necessary
        {
            ListIterator iter = children.listIterator ();

            while (iter.hasNext ())
            {
                SimpleSchedStream child = (SimpleSchedStream) iter.next ();
                ASSERT (child);

                // add the initialization schedule:
                initSchedule.add (child.getInitSchedule ());

                // add the steady schedule an appropriate number of times:
                Integer numExecutions = (Integer)numExecutionsForInit.get (child);
                ASSERT (numExecutions);

                int n = numExecutions.intValue ();
                for ( ; n > 0 ; n--)
                {
                    initSchedule.add (child.getSteadySchedule ());
                }
            }
        }

        // compute the amount of data consumed by initialization
        // this is easy, 'cause I already know how many times my first child
        // will need to get executed, and how much data it consumes on its
        // own initialization
        if (!children.isEmpty ())
        {
            // get my first child
            SimpleSchedStream firstChild = (SimpleSchedStream) children.get (0);
            ASSERT (firstChild);

            // now get the amount of data pulled when initializing
            int initData = firstChild.getInitDataCount ();

            // and the amount of data pulled when filling the pipeline
            int fillData = ((Integer)numExecutionsForInit.get (firstChild)).intValue () *
                           firstChild.getConsumption ();

            // and thus I have the amount of data needed:
            initDataCount = initData + fillData;
        } else {
            initDataCount = 0;
        }

        // compute the steady state schedule
        // this one is quite easy - I go through the children and execute them
        // an appropriate number of times
        {
            ListIterator iter = children.listIterator ();

            while (iter.hasNext ())
            {
                SimpleSchedStream child = (SimpleSchedStream) iter.next ();
                ASSERT (child);

                BigInteger numExecutions = child.getNumExecutions ();

                // enter all the appropriate sub-schedules into the schedule
                while (numExecutions.signum () != 0)
                {
                    steadySchedule.add (child.getSteadySchedule ());
                    numExecutions = numExecutions.subtract (BigInteger.ONE);
                }
            }

        }

        // now I just need to compute the buffer sizes and I'm DONE!
        // this should be done by getting max of:
        //  - the amount of data produced during steady state execution
        //    added to amount of data left in the buffer after initialization
        //  - the amount of data left in a buffer after running the producer
        //    during initialization
        // I don't need to worry about peeking explicitly, 'cause it is already
        // implicitly taken care of in the intialization!
        // note: I may want to keep the buffer either n*steadyProduction or 2^n
        if (!children.isEmpty ())
        {
            ListIterator iter = children.listIterator ();
            SimpleSchedStream prevChild = (SimpleSchedStream) iter.next ();
            ASSERT (prevChild);

            while (iter.hasNext ())
            {
                SimpleSchedStream child = (SimpleSchedStream) iter.next ();
                ASSERT (child);

                // compute the amount of data consumed (produced)
                // during steady state execution
                BigInteger steadyStateProd = child.getNumExecutions ().multiply (BigInteger.valueOf (child.getConsumption ()));

                // compute the amount of data produced during initialization
                BigInteger initProdSize;
                {
                    int prod = ((Integer)numExecutionsForInit.get (prevChild)).intValue () *
                               prevChild.getProduction ();
                    initProdSize = BigInteger.valueOf (prod);
                }

                // compute the amount of data consumed during intitialization
                BigInteger initConsumeSize;
                {
                    int initConsume = child.getInitDataCount ();
                    int fillConsume = ((Integer)numExecutionsForInit.get (child)).intValue () *
                                      child.getConsumption ();
                    initConsumeSize = BigInteger.valueOf (initConsume + fillConsume);
                }

                BigInteger bufferSize = initProdSize.max (initProdSize.subtract (initConsumeSize).add (steadyStateProd));
                scheduler.schedule.setBufferSize (prevChild.getStreamObject (), child.getStreamObject (), bufferSize);

                prevChild = child;
            }
        }


        /*
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
        */
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
        return initDataCount;
    }
}