package streamit.scheduler.simple;

import streamit.scheduler.SchedLoop;
import streamit.scheduler.SchedJoinType;
import streamit.scheduler.SchedSplitType;
import streamit.scheduler.SchedStream;

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

import java.math.BigInteger;


class SimpleSchedLoop extends SchedLoop implements SimpleSchedStream
{
    SimpleHierarchicalScheduler scheduler;
    private List steadySchedule = null;
    private List initSchedule = null;
    int initDataConsumption = 0;
    int initDataProduction = 0;

    SimpleSchedLoop (SimpleHierarchicalScheduler scheduler, Object stream, SchedJoinType join, SchedStream body, SchedSplitType split, SchedStream loop, int delay)
    {
        super (stream, join, body, split, loop, delay);

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

        // now initialize all the children appropriately
        {
            ASSERT (getLoopBody () instanceof SimpleSchedStream);
            ((SimpleSchedStream) getLoopBody ()).computeSchedule ();

            ASSERT (getLoopFeedbackPath () instanceof SimpleSchedStream);
            ((SimpleSchedStream) getLoopFeedbackPath ()).computeSchedule ();
        }

        // counters for how much data is buffered
        // before each element of the loop
        BigInteger bodyBuffer;
        BigInteger splitBuffer;
        BigInteger loopBuffer;
        BigInteger joinBuffer;

        // keep track of maximal size of buffer needed (will NOT start every
        // execution on the same buffer boundary!)
        BigInteger maxSplitBuffer;
        BigInteger maxJoinBuffer;
        BigInteger maxBodyBuffer;
        BigInteger maxLoopBuffer;

        // compute the initialization schedule
        {
            int nInitRunSplit;
            int nInitRunBody;
            int nInitRunJoin;

            SimpleSchedStream body = (SimpleSchedStream) getLoopBody ();
            SimpleSchedStream feedback = (SimpleSchedStream)getLoopFeedbackPath ();

            ASSERT (body);
            ASSERT (feedback);

            // figure out how many times the split needs to run:
            // and initDataProduction for the entire loop
            {
                int feedbackDataNeeded = (feedback.getPeekConsumption () - feedback.getConsumption ())
                                       + feedback.getInitDataConsumption ();

                int splitProduction = getLoopSplit ().getOutputWeight (1);
                nInitRunSplit = (feedbackDataNeeded + splitProduction - 1) / splitProduction;
                loopBuffer = BigInteger.valueOf (nInitRunSplit * splitProduction - feedback.getInitDataConsumption ());
                maxLoopBuffer = BigInteger.valueOf (nInitRunSplit * splitProduction);
                initDataProduction = nInitRunSplit * getLoopSplit ().getOutputWeight (0);
            }

            // figure out how many times the body needs to be run:
            {
                int splitDataNeeded = getLoopFeedbackPath ().getConsumption () * nInitRunSplit;
                int bodyProduction = body.getProduction ();
                nInitRunBody = (splitDataNeeded + (bodyProduction - 1) - body.getInitDataProduction ()) / bodyProduction;
                splitBuffer = BigInteger.valueOf (nInitRunBody * bodyProduction + body.getInitDataProduction ()
                                                - nInitRunSplit * getLoopSplit ().getRoundConsumption ());
                maxSplitBuffer = BigInteger.valueOf (nInitRunBody * bodyProduction + body.getInitDataProduction ());
            }

            // figure out how many times the join needs to be run
            // and initDataConsumption for the entire loop
            {
                int bodyDataNeeded = (body.getPeekConsumption () - body.getConsumption ())
                                   + body.getInitDataConsumption ()
                                   + nInitRunBody * body.getProduction ();
                int joinProduction = getLoopJoin ().getRoundProduction ();
                nInitRunJoin = (bodyDataNeeded + joinProduction - 1) / joinProduction;
                bodyBuffer = BigInteger.valueOf (nInitRunJoin * joinProduction
                                               - body.getInitDataConsumption ()
                                               - nInitRunBody * body.getConsumption ());
                maxBodyBuffer = BigInteger.valueOf (nInitRunJoin * joinProduction);
                initDataConsumption = nInitRunJoin * getLoopJoin ().getInputWeight (0);
            }

            // now setup buffer sizes for the join
            {
                int loopInitProduction = feedback.getInitDataProduction ();
                int joinConsumption = getLoopJoin ().getInputWeight (1);
                joinBuffer = BigInteger.valueOf (getLoopDelay () + loopInitProduction - (nInitRunJoin * joinConsumption));
                maxJoinBuffer = joinBuffer.max (BigInteger.valueOf (getLoopDelay ()));

                // check if this is a legal schedule in the first place
                if (getLoopDelay () - nInitRunJoin * joinConsumption < 0)
                {
                    schedulingDifficulty ();
                }
            }

            // finally, actually create the init schedule
            {
                for ( ; nInitRunJoin > 0; nInitRunJoin--)
                {
                    initSchedule.add (getLoopJoin ().getJoinObject ());
                }

                if (body.getInitSchedule () != null)
                {
                    initSchedule.add (body.getInitSchedule ());
                }

                for ( ; nInitRunBody > 0; nInitRunBody--)
                {
                    initSchedule.add (body.getSteadySchedule ());
                }

                for ( ; nInitRunSplit > 0; nInitRunSplit--)
                {
                    initSchedule.add (getLoopSplit ().getSplitObject ());
                }

                if (feedback.getInitSchedule () != null)
                {
                    initSchedule.add (feedback.getInitSchedule ());
                }
            }
        }

        // store the starting buffer sizes:
        BigInteger startBodyBuffer = bodyBuffer;
        BigInteger startSplitBuffer = splitBuffer;
        BigInteger startLoopBuffer = loopBuffer;
        BigInteger startJoinBuffer = joinBuffer;

        // counters for how many times each component of the loop gets executed
        BigInteger splitExecutions = getNumSplitExecutions ();
        BigInteger joinExecutions = getNumJoinExecutions ();
        BigInteger bodyExecutions = getNumBodyExecutions ();
        BigInteger loopExecutions = getNumLoopExecutions ();

        // there are four elements of the loop that need to be completed
        // everytime one of these elements decreases its num executions
        // to 0 (it's been completely scheduled), done variable will
        // be increased.  When done reaches 4, all four elements
        // will have finished scheduling.
        int done = 0;

        while (done != 4)
        {
            // keep track if I've moved forward, or if I'm completely stuck
            // and should exit with an error msg.
            boolean movedForward = false;

            // attempt to push some data through the feedback loop:
            {
                while (joinBuffer.compareTo (BigInteger.valueOf (getLoopJoin ().getInputWeight (1))) >= 0
                       && !joinExecutions.equals (BigInteger.ZERO))
                {
                    // move the data forward
                    movedForward = true;
                    steadySchedule.add (getLoopJoin ().getJoinObject ());
                    joinBuffer = joinBuffer.subtract (BigInteger.valueOf (getLoopJoin ().getInputWeight (1)));
                    bodyBuffer = bodyBuffer.add (BigInteger.valueOf (getLoopJoin ().getRoundProduction ()));

                    // check if done, and indicate if so
                    joinExecutions = joinExecutions.subtract (BigInteger.ONE);
                    if (joinExecutions.equals (BigInteger.ZERO))
                    {
                        done ++;
                    }
                }

                // figure out the max size for a body buffer
                maxBodyBuffer = maxBodyBuffer.max (bodyBuffer);
            }

            // attempt to push some data through the body of the loop:
            {
                while (bodyBuffer.compareTo (BigInteger.valueOf (getLoopBody ().getPeekConsumption ())) >= 0
                       && !bodyExecutions.equals (BigInteger.ZERO))
                {
                    movedForward = true;
                    steadySchedule.add (((SimpleSchedStream)getLoopBody ()).getSteadySchedule ());
                    bodyBuffer = bodyBuffer.subtract (BigInteger.valueOf (getLoopBody ().getConsumption ()));
                    splitBuffer = splitBuffer.add (BigInteger.valueOf (getLoopBody ().getProduction ()));

                    // check if done, and indicate if so
                    bodyExecutions = bodyExecutions.subtract (BigInteger.ONE);
                    if (bodyExecutions.equals (BigInteger.ZERO))
                    {
                        done ++;
                    }
                }

                // figure out the max size for a split buffer
                maxSplitBuffer = maxSplitBuffer.max (splitBuffer);
            }

            // attempt to push some data through the split of the loop:
            {
                while (splitBuffer.compareTo (BigInteger.valueOf (getLoopSplit ().getRoundConsumption ())) >= 0
                       && !splitExecutions.equals (BigInteger.ZERO))
                {
                    movedForward = true;
                    steadySchedule.add (getLoopSplit ().getSplitObject ());
                    splitBuffer = splitBuffer.subtract (BigInteger.valueOf (getLoopSplit ().getRoundConsumption ()));
                    loopBuffer = loopBuffer.add (BigInteger.valueOf (getLoopSplit ().getOutputWeight (1)));

                    // check if done, and indicate if so
                    splitExecutions = splitExecutions.subtract (BigInteger.ONE);
                    if (splitExecutions.equals (BigInteger.ZERO))
                    {
                        done ++;
                    }
                }

                // figure out the max size for a loop buffer
                maxLoopBuffer = maxLoopBuffer.max (loopBuffer);
            }

            // attempt to push some data through the feedback path of the loop:
            {
                while (loopBuffer.compareTo (BigInteger.valueOf (getLoopFeedbackPath ().getPeekConsumption ())) >= 0
                       && !loopExecutions.equals (BigInteger.ZERO))
                {
                    movedForward = true;
                    steadySchedule.add (((SimpleSchedStream)getLoopFeedbackPath ()).getSteadySchedule ());
                    loopBuffer = loopBuffer.subtract (BigInteger.valueOf (getLoopFeedbackPath ().getConsumption ()));
                    joinBuffer = joinBuffer.add (BigInteger.valueOf (getLoopFeedbackPath ().getProduction ()));

                    // check if done, and indicate if so
                    loopExecutions = loopExecutions.subtract (BigInteger.ONE);
                    if (loopExecutions.equals (BigInteger.ZERO))
                    {
                        done ++;
                    }
                }

                // figure out the max size for a body buffer
                maxJoinBuffer = maxJoinBuffer.max (joinBuffer);
            }

            if (!movedForward)
            {
                schedulingDifficulty ();
            }
        }

        // make sure that after execution of this schedule, the size of buffers
        // left over is SAME as when we started!
        ASSERT (bodyBuffer.equals (startBodyBuffer));
        ASSERT (splitBuffer.equals (startSplitBuffer));
        ASSERT (loopBuffer.equals (startLoopBuffer));
        ASSERT (joinBuffer.equals (startJoinBuffer));

        // store the buffer size information
        scheduler.schedule.setBufferSize (getLoopJoin ().getJoinObject (), getLoopBody ().getStreamObject (), maxBodyBuffer);
        scheduler.schedule.setBufferSize (getLoopBody ().getStreamObject (), getLoopSplit ().getSplitObject (), maxSplitBuffer);
        scheduler.schedule.setBufferSize (getLoopSplit ().getSplitObject (), getLoopFeedbackPath ().getStreamObject (), maxLoopBuffer);
        scheduler.schedule.setBufferSize (getLoopFeedbackPath ().getStreamObject (), getLoopJoin ().getJoinObject (), maxJoinBuffer);
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
        ASSERT (initDataProduction >= 0);
        return initDataProduction;
    }

    void schedulingDifficulty ()
    {
        // get the name of the loop class - this will be useful
        // for debugging
        String className = getStreamObject ().getClass ().getName ();
        ERROR ("Couldn't schedule loop " + className + ".\n" +
               "This loop is not necessarily impossible to schedule, " +
               "but this scheduler isn't intelligent enough to do it");
    }
}