package streamit;

import streamit.Pipeline;
import streamit.scheduler.simple.SimpleHierarchicalSchedulerPow2;
import streamit.scheduler.simple.SimpleHierarchicalScheduler;
import streamit.scheduler.SchedStream;

import java.util.List;
import java.util.ListIterator;

/**
 * Main class
 */
public class StreamIt extends Pipeline
{
    int numExecutions = 0;
    void runSchedule (Object schedule)
    {
        if (schedule instanceof Operator)
        {
            numExecutions ++;
            if (numExecutions == 10000)
            {
                System.out.print (".");
                numExecutions = 0;
            }
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

    public void run ()
    {
        run (null);
    }

    // just a runtime hook to run the stream
    public void run(String args [])
    {
        boolean scheduledRun = true;
        boolean printGraph = false;
        boolean doRun = true;

        // read the args:
        if (args != null)
        {
            int length = args.length;
            int index;
            for (index = 0; index < length; index++)
            {
                if (args [index].equals ("-nosched"))
                {
                    scheduledRun = false;
                } else
                if (args [index].equals ("-printgraph"))
                {
                    printGraph = true;
                } else
                if (args [index].equals ("-norun"))
                {
                    doRun = false;
                } else {
                    ERROR ("Unrecognized argument: " + args [index] + ".");
                }
            }
        }

        setupOperator ();

        ASSERT (getInputChannel () == null);
        ASSERT (getOutputChannel () == null);

        // setup the scheduler
        if (printGraph)
        {
            scheduler = new SimpleHierarchicalScheduler ();

            SchedStream stream;
            stream = (SchedStream) constructSchedule ();
            ASSERT (stream);

            scheduler.useStream (stream);
            scheduler.print (System.out);
        }

        if (!doRun) System.exit (0);

        // setup the scheduler
        if (scheduledRun)
        {
            scheduler = new SimpleHierarchicalSchedulerPow2 ();

            SchedStream stream;
            stream = (SchedStream) constructSchedule ();
            ASSERT (stream);

            scheduler.useStream (stream);
            scheduler.computeSchedule ();

            // setup the buffer lengths for the stream setup here:
            setupBufferLengths (scheduler.getSchedule ());

            // run the init schedule:
            runSchedule (scheduler.getSchedule ().getInitSchedule ());

            // and run the steady schedule forever:
            while (true)
            {
                runSchedule (scheduler.getSchedule ().getSteadySchedule ());
            }
        } else {
            while (true)
            {
                runSinks ();
                drainChannels ();
            }
        }
    }
}

