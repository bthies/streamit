package streamit;

import streamit.Pipeline;
import streamit.scheduler.simple.SimpleHierarchicalSchedulerPow2;
import streamit.scheduler.simple.SimpleHierarchicalScheduler;
import streamit.scheduler.SchedStream;
import streamit.scheduler.SchedRepSchedule;

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
        if (schedule instanceof Filter)
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
        } else
        if (schedule instanceof SchedRepSchedule)
        {
            SchedRepSchedule repSchedule = (SchedRepSchedule) schedule;

            int nTimes = repSchedule.getTotalExecutions ().intValue ();
            for ( ; nTimes > 0; nTimes--)
            {
                runSchedule (repSchedule.getOriginalSchedule ());
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
        int nIters = -1;

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
                if (args [index].equals ("-i"))
                {
                    index++;
                    nIters = Integer.valueOf (args[index]);
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
            while (nIters != 0)
            {
                runSchedule (scheduler.getSchedule ().getSteadySchedule ());
                if (nIters > 0) nIters--;
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

