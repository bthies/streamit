package streamit;

import streamit.Pipeline;
import streamit.scheduler.simple.SimpleHierarchicalScheduler;
import streamit.scheduler.SchedStream;

/**
 * Main class
 */
public class StreamIt extends Pipeline
{
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

        ASSERT (streamInput == null);
        ASSERT (streamOutput == null);

        // execute the stream here
        while (true)
        {
            runSinks ();
            drainChannels ();
        }
    }
}
