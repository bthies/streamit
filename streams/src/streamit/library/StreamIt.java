package streamit;

import streamit.Pipeline;
import streamit.scheduler.simple.SimpleHierarchicalScheduler;
import streamit.scheduler.SchedStream;

import java.util.List;
import java.util.ListIterator;

/**
 * Main class
 */
public class StreamIt extends Pipeline
{
    void runSchedule (Object schedule)
    {
        if (schedule instanceof Operator)
        {
            ((Operator) schedule).work ();
        } else
        if (schedule instanceof List)
        {
            List list = (List) schedule;
            ListIterator iter = list.listIterator ();

            while (iter.hasNext ())
            {
                Object child = iter.next ();
                ASSERT (child);
                runSchedule (child);
            }
        } else ASSERT (false);
    }

    // just a runtime hook to run the stream
    public void run()
    {
        setupOperator ();

        // setup the scheduler
        {
            scheduler = new SimpleHierarchicalScheduler ();

            SchedStream stream;
            stream = (SchedStream) constructSchedule ();
            ASSERT (stream);

            scheduler.useStream (stream);
            scheduler.computeSchedule ();
        }

        ASSERT (getInputChannel () == null);
        ASSERT (getOutputChannel () == null);

        // setup the buffer lengths for the stream setup here:
        setupBufferLengths (scheduler.getSchedule ());

        // run the init schedule:
        runSchedule (scheduler.getSchedule ().getInitSchedule ());

        // and run the steady schedule forever:
        while (true)
        {
            runSchedule (scheduler.getSchedule ().getSteadySchedule ());
        }

        // not necessary anymore
        /*
        while (true)
        {
            runSinks ();
            drainChannels ();
        }
        */
    }
}

