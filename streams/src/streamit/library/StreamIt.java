package streamit;

import java.util.List;
import java.util.ListIterator;
import java.lang.reflect.Array;
import streamit.scheduler2.iriter.Iterator;
import streamit.iriter.StreamFactory;
import streamit.scheduler2.base.StreamInterface;
import streamit.scheduler2.ScheduleBuffers;
import streamit.scheduler2.Schedule;
import streamit.misc.UniquePairContainer;
import streamit.misc.Pair;

/**
 * Main class
 */
public class StreamIt extends Pipeline
{
    int numExecutions = 0;
    boolean printdot = false;
    void runSchedule(Object schedule, Object function)
    {
        if (schedule instanceof Operator)
        {
            numExecutions++;
            if (numExecutions == 10000)
            {
                if (printdot)
                    System.out.print(".");
                numExecutions = 0;
            }
            int inputCount = 0, outputCount = 0;
            if (schedule instanceof Filter)
            {
                Stream stream = (Stream) schedule;
                if (stream.getInputChannel() != null)
                {
                    inputCount = stream.getInputChannel().getItemsPopped();
                }
                if (stream.getOutputChannel() != null)
                {
                    outputCount =
                        stream.getOutputChannel().getItemsPushed();
                }
            }

            Operator oper = (Operator) schedule;
            if (oper instanceof Filter)
                oper.work();
            else if (oper instanceof SplitJoin || oper instanceof FeedbackLoop)
            {
                ASSERT(function instanceof Operator);
                ((Operator) function).work();
            }
            else
                ASSERT(false);

            if (schedule instanceof Filter)
            {
                Stream stream = (Stream) schedule;
                int newInputCount, newOutputCount;
                if (stream.getInputChannel() != null)
                {
                    newInputCount =
                        stream.getInputChannel().getItemsPopped();
                    ASSERT(
                        newInputCount - inputCount
                            == stream.getInputChannel().getPopCount(),
                        "This probably means that you declared the wrong pop rate for "
                            + stream
                            + "\n  have newInput=="
                            + newInputCount
                            + " and inputCount=="
                            + inputCount
                            + " and popCount=="
                            + stream.getInputChannel().getPopCount()
                            + " on Stream "
                            + stream);
                }
                if (stream.getOutputChannel() != null)
                {
                    newOutputCount =
                        stream.getOutputChannel().getItemsPushed();
                    ASSERT(
                        newOutputCount - outputCount
                            == stream.getOutputChannel().getPushCount(),
                        "This probably means that you declared the wrong push rate for "
                            + stream
                            + "\n"
                            + "Pushed: "
                            + newOutputCount
                            + " and outputCount: "
                            + outputCount
                            + " but declared: "
                            + stream.getOutputChannel().getPushCount()
                            + " on Stream "
                            + stream);
                }

            }
        }
    }
    void runSchedule(Object schedule)
    {
        if (schedule instanceof Schedule)
        {
            Schedule repSchedule = (Schedule) schedule;

            int nTimes = repSchedule.getNumReps();
            for (; nTimes > 0; nTimes--)
            {
                if (repSchedule.isBottomSchedule())
                {
                    runSchedule(
                        repSchedule.getStream().getObject(),
                        repSchedule.getWorkFunc());
                }
                else
                {
                    int nSched;
                    for (nSched = 0;
                        nSched < repSchedule.getNumPhases();
                        nSched++)
                    {
                        runSchedule(repSchedule.getSubSched(nSched));
                    }
                }
            }

        }
        else
            ASSERT(false);
    }

    /* removing this to force people to pass arguments
    public void run ()
    {
        run(null);
    }
    */

    // just a runtime hook to run the stream
    public void run(String args[])
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
                if (args[index].equals("-nosched"))
                {
                    scheduledRun = false;
                }
                else if (args[index].equals("-printgraph"))
                {
                    printGraph = true;
                }
                else if (args[index].equals("-i"))
                {
                    index++;
                    nIters = Integer.valueOf(args[index]).intValue();
                }
                else if (args[index].equals("-printdot"))
                {
                    printdot = true;
                }
                else if (args[index].equals("-norun"))
                {
                    doRun = false;
                }
                else
                {
                    ERROR("Unrecognized argument: " + args[index] + ".");
                }
            }
        }

        setupOperator();

        ASSERT(
            getInputChannel() == null,
            "The toplevel stream can't have any input or output channels,\n"
                + "but in this program there is an input to the first filter.");
        ASSERT(
            getOutputChannel() == null,
            "The toplevel stream can't have any input or output channels,\n"
                + "but in this program there is an output of the last filter.");

        // setup the scheduler
        if (printGraph)
        {
            // this is not implemented yet.  when I'm done with
            // some certain amount of the scheduler, I can start
            // using the iterators to do this properly.
            ASSERT(false);
        }

        if (!doRun)
            System.exit(0);

        // setup the scheduler
        if (scheduledRun)
        {
            // not implemented yet. waiting for the scheduler to
            // be done.
            Iterator selfIter = new streamit.iriter.Iterator(this);
            StreamFactory factory = new StreamFactory();
            StreamInterface selfStream = factory.newFrom(selfIter);
            selfStream.computeSchedule();
            Schedule initSched = selfStream.getInitSchedule();
            Schedule steadySched = selfStream.getSteadySchedule();

            ScheduleBuffers buffers = new ScheduleBuffers(selfIter);
            buffers.computeBuffersFor(initSched);
            buffers.computeBuffersFor(steadySched);

            // write equals and hashCode functions for operators!
            // this will solve all equality problems.

            /*
            scheduler = new SimpleHierarchicalScheduler ();
            
            SchedStream stream;
            stream = (SchedStream) constructSchedule ();
            ASSERT (stream);
            
            scheduler.useStream (stream);
            scheduler.computeSchedule ();
            */

            // setup the buffer lengths for the stream setup here:
            setupBufferLengths(buffers);

            // run the init schedule:
            runSchedule(initSched);

            // and run the steady schedule forever:
            while (nIters != 0)
            {
                runSchedule(steadySched);
                if (nIters > 0)
                    nIters--;
            }
        }
        else
        {
            while (true)
            {
                runSinks();
                drainChannels();
            }
        }
    }
}
