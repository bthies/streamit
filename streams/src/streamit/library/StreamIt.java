package streamit;

import streamit.Pipeline;
import java.util.List;
import java.util.ListIterator;
import java.lang.reflect.Array;
import streamit.iriter.Iterator;
import streamit.iriter.StreamFactory;
import streamit.scheduler.base.StreamInterface;

import streamit.scheduler.Schedule;

/**
 * Main class
 */
public class StreamIt extends Pipeline
{
    int numExecutions = 0;
    boolean printdot = false;
    void runSchedule (Object schedule)
    {
        if (schedule instanceof Operator)
        {
            numExecutions ++;
            if (numExecutions == 10000)
            {
                if (printdot) System.out.print (".");
                numExecutions = 0;
            }
            int inputCount = 0, outputCount = 0;
            if (schedule instanceof Stream)
            {
                Stream stream = (Stream) schedule;
                if (stream.getInputChannel () != null)
                {
                	inputCount = stream.getInputChannel ().getItemsPopped ();
                }
                if (stream.getOutputChannel () != null)
                {
                	outputCount = stream.getOutputChannel ().getItemsPushed ();
                }
            }
            
            Operator oper = (Operator)schedule;
            oper.work ();
            
            if (schedule instanceof Stream)
            {
                Stream stream = (Stream) schedule;
                int newInputCount, newOutputCount;
                if (stream.getInputChannel () != null)
                {
                	newInputCount = stream.getInputChannel ().getItemsPopped ();
                	ASSERT (newInputCount - inputCount == stream.getInputChannel ().getPopCount(), "This probably means that you declared the wrong pop rate for " + stream + "\n  have newInput==" + newInputCount +
				" and inputCount==" + inputCount +
				" and popCount==" + stream.getInputChannel ().getPopCount() +
				" on Stream " + stream); 
                }
                if (stream.getOutputChannel () != null)
                {
                	newOutputCount = stream.getOutputChannel ().getItemsPushed ();
                	ASSERT (newOutputCount - outputCount == stream.getOutputChannel ().getPushCount()); 
                }
                
            }
        } else
        if (schedule instanceof Array)
        {
            Object [] array = (Object []) schedule;
            int size = array.length;


            for (int index = 0; index < size; index++)
            {
                Object child = array [index];
                ASSERT (child);
                runSchedule (child);
            }
        } else
        if (schedule instanceof Schedule)
        {
            Schedule repSchedule = (Schedule) schedule;
            
            if (repSchedule.isBottomSchedule ())
            {
                runSchedule (repSchedule.getWorkFunc ());
            } else {
                int nTimes = repSchedule.getNumReps ();
                for ( ; nTimes > 0; nTimes--)
                {
                    int nSched;
                    for (nSched = 0; nSched < repSchedule.getNumPhases(); nSched++)
                    {
                        runSchedule (repSchedule.getSubSched (nSched));
                    }
                }
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
                    nIters = Integer.valueOf (args[index]).intValue ();
                } else
                if (args [index].equals ("-printdot"))
                {
                    printdot = true;
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
            // this is not implemented yet.  when I'm done with
            // some certain amount of the scheduler, I can start
            // using the iterators to do this properly.
            ASSERT (false);
        }

        if (!doRun) System.exit (0);

        // setup the scheduler
        if (scheduledRun)
        {
            // not implemented yet. waiting for the scheduler to
            // be done.
            Iterator selfIter = new Iterator (this);
            StreamFactory factory = new StreamFactory ();
            StreamInterface selfStream = factory.newFrom (selfIter);
            selfStream.computeSchedule ();
            ASSERT (false);
            
            /*
            scheduler = new SimpleHierarchicalScheduler ();
    
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
            */
        } else {
            while (true)
            {
                runSinks ();
                drainChannels ();
            }
        }
    }
}

