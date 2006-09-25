package streamit.scheduler1.simple;

import streamit.scheduler1.SchedLoop;
import streamit.scheduler1.SchedJoinType;
import streamit.scheduler1.SchedSplitType;
import streamit.scheduler1.SchedStream;
import streamit.scheduler1.SchedRepSchedule;

import java.util.List;
import java.util.LinkedList;

import java.math.BigInteger;

// BUGBUG these are for my little hack of DecoderFeedback
import streamit.scheduler1.SchedPipeline;
import streamit.scheduler1.SchedSplitJoin;

class SimpleSchedLoop extends SchedLoop implements SimpleSchedStream
{
    SimpleHierarchicalScheduler scheduler;
    private List<Object> steadySchedule = null;
    private List<Object> initSchedule = null;
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
            steadySchedule = new LinkedList<Object> ();
            initSchedule = new LinkedList<Object> ();
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
                if (nInitRunJoin > 0)
                    {
                        initSchedule.add (new SchedRepSchedule (BigInteger.valueOf (nInitRunJoin), getLoopJoin ().getJoinObject ()));
                    }

                if (body.getInitSchedule () != null)
                    {
                        initSchedule.add (body.getInitSchedule ());
                    }

                if (nInitRunBody > 0)
                    {
                        initSchedule.add (new SchedRepSchedule (BigInteger.valueOf (nInitRunBody), body.getSteadySchedule ()));
                    }

                if (nInitRunSplit > 0)
                    {
                        initSchedule.add (new SchedRepSchedule (BigInteger.valueOf (nInitRunSplit), getLoopSplit ().getSplitObject ()));
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
                    int nJoinExecutions = 0;
                    while (joinBuffer.compareTo (BigInteger.valueOf (getLoopJoin ().getInputWeight (1))) >= 0
                           && !joinExecutions.equals (BigInteger.ZERO))
                        {
                            // move the data forward
                            movedForward = true;
                            nJoinExecutions ++;
                            joinBuffer = joinBuffer.subtract (BigInteger.valueOf (getLoopJoin ().getInputWeight (1)));
                            bodyBuffer = bodyBuffer.add (BigInteger.valueOf (getLoopJoin ().getRoundProduction ()));

                            // check if done, and indicate if so
                            joinExecutions = joinExecutions.subtract (BigInteger.ONE);
                            if (joinExecutions.equals (BigInteger.ZERO))
                                {
                                    done ++;
                                }
                        }

                    // actually add the executions to the schedule
                    if (nJoinExecutions > 0)
                        {
                            steadySchedule.add (new SchedRepSchedule (BigInteger.valueOf (nJoinExecutions), getLoopJoin ().getJoinObject ()));
                        }

                    // figure out the max size for a body buffer
                    maxBodyBuffer = maxBodyBuffer.max (bodyBuffer);
                }

                // attempt to push some data through the body of the loop:
                {
                    int nBodyExecutions = 0;
                    while (bodyBuffer.compareTo (BigInteger.valueOf (getLoopBody ().getPeekConsumption ())) >= 0
                           && !bodyExecutions.equals (BigInteger.ZERO))
                        {
                            movedForward = true;
                            nBodyExecutions ++;
                            bodyBuffer = bodyBuffer.subtract (BigInteger.valueOf (getLoopBody ().getConsumption ()));
                            splitBuffer = splitBuffer.add (BigInteger.valueOf (getLoopBody ().getProduction ()));

                            // check if done, and indicate if so
                            bodyExecutions = bodyExecutions.subtract (BigInteger.ONE);
                            if (bodyExecutions.equals (BigInteger.ZERO))
                                {
                                    done ++;
                                }
                        }

                    // actually add the executions to the schedule
                    if (nBodyExecutions > 0)
                        {
                            steadySchedule.add (new SchedRepSchedule (BigInteger.valueOf (nBodyExecutions), ((SimpleSchedStream)getLoopBody ()).getSteadySchedule ()));
                        }

                    // figure out the max size for a split buffer
                    maxSplitBuffer = maxSplitBuffer.max (splitBuffer);
                }

                // attempt to push some data through the split of the loop:
                {
                    int nSplitExecutions = 0;
                    while (splitBuffer.compareTo (BigInteger.valueOf (getLoopSplit ().getRoundConsumption ())) >= 0
                           && !splitExecutions.equals (BigInteger.ZERO))
                        {
                            movedForward = true;
                            nSplitExecutions ++;
                            splitBuffer = splitBuffer.subtract (BigInteger.valueOf (getLoopSplit ().getRoundConsumption ()));
                            loopBuffer = loopBuffer.add (BigInteger.valueOf (getLoopSplit ().getOutputWeight (1)));

                            // check if done, and indicate if so
                            splitExecutions = splitExecutions.subtract (BigInteger.ONE);
                            if (splitExecutions.equals (BigInteger.ZERO))
                                {
                                    done ++;
                                }
                        }

                    // actually add the executions to the schedule
                    if (nSplitExecutions > 0)
                        {
                            steadySchedule.add (new SchedRepSchedule (BigInteger.valueOf (nSplitExecutions), getLoopSplit ().getSplitObject ()));
                        }

                    // figure out the max size for a loop buffer
                    maxLoopBuffer = maxLoopBuffer.max (loopBuffer);
                }

                // attempt to push some data through the feedback path of the loop:
                {
                    int nFeedbackPathExecutions = 0;
                    while (loopBuffer.compareTo (BigInteger.valueOf (getLoopFeedbackPath ().getPeekConsumption ())) >= 0
                           && !loopExecutions.equals (BigInteger.ZERO))
                        {
                            movedForward = true;
                            nFeedbackPathExecutions ++;
                            loopBuffer = loopBuffer.subtract (BigInteger.valueOf (getLoopFeedbackPath ().getConsumption ()));
                            joinBuffer = joinBuffer.add (BigInteger.valueOf (getLoopFeedbackPath ().getProduction ()));

                            // check if done, and indicate if so
                            loopExecutions = loopExecutions.subtract (BigInteger.ONE);
                            if (loopExecutions.equals (BigInteger.ZERO))
                                {
                                    done ++;
                                }
                        }

                    // actually add the executions to the schedule
                    if (nFeedbackPathExecutions > 0)
                        {
                            steadySchedule.add (new SchedRepSchedule (BigInteger.valueOf (nFeedbackPathExecutions), ((SimpleSchedStream)getLoopFeedbackPath ()).getSteadySchedule ()));
                        }

                    // figure out the max size for a body buffer
                    maxJoinBuffer = maxJoinBuffer.max (joinBuffer);
                }

                if (!movedForward)
                    {
                        schedulingDifficulty ();
                        // BUGBUG remove this return - it's pretty useless and confusing!
                        return;
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
        String name;
        name = getStreamObject ().getClass ().getName ();

        // BUGBUG hack the DecoderFeedback with fine grained scheduling
        try {
            if (getStreamObject ().getClass ().getMethod ("getName", (java.lang.Class[])null) != null) {
                // if we're coming from the compiler, get the name of the
                // SIR object
                name = (String) getStreamObject ().getClass ().getMethod ("getName", (java.lang.Class[])null).invoke 
                    (getStreamObject (), (java.lang.Object[])null);
            }
        } catch (Throwable e)
            { }
        if (name.startsWith ("DecoderFeedback")) {
            fakeDecoderFeedback ();
            return;
        }

        ERROR ("Couldn't schedule loop " + name + ".\n" +
               "This loop is not necessarily impossible to schedule, " +
               "but this scheduler isn't intelligent enough to do it");
    }

    void fakeDecoderFeedback ()
    {
        steadySchedule.clear ();

        SchedJoinType myJoiner = getLoopJoin ();
        SimpleSchedStream myBody = (SimpleSchedStream) getLoopBody ();
        SchedSplitType mySplitter = getLoopSplit ();

        SchedPipeline myLoop = (SchedPipeline) getLoopFeedbackPath ();
        SchedSplitJoin LTPSplitJoin = (SchedSplitJoin) myLoop.getChild (0);
        SchedStream LTPFilter = (SchedStream) myLoop.getChild (1);

        steadySchedule.add (((SimpleSchedStream)LTPSplitJoin.getChild (0)).getSteadySchedule ());

        int x;
        for (x = 0; x < 4; x++)
            {
                steadySchedule.add (myJoiner.getStreamObject ());
                steadySchedule.add (myBody.getSteadySchedule ());
                steadySchedule.add (new SchedRepSchedule (BigInteger.valueOf (160), mySplitter.getStreamObject ()));
                steadySchedule.add (new SchedRepSchedule (BigInteger.valueOf (160), LTPSplitJoin.getSplitType ().getStreamObject ()));
                steadySchedule.add (new SchedRepSchedule (BigInteger.valueOf (160), ((SimpleSchedStream)LTPSplitJoin.getChild (1)).getSteadySchedule ()));
                steadySchedule.add (LTPSplitJoin.getJoinType ().getStreamObject ());
                steadySchedule.add (((SimpleSchedStream)LTPFilter).getSteadySchedule ());
            }

        scheduler.schedule.setBufferSize (myJoiner.getStreamObject (), myBody.getStreamObject (), BigInteger.valueOf (41));
        scheduler.schedule.setBufferSize (myBody.getStreamObject (), mySplitter.getStreamObject (), BigInteger.valueOf (160));
        scheduler.schedule.setBufferSize (mySplitter.getStreamObject (), myLoop.getStreamObject (), BigInteger.valueOf (160));
        scheduler.schedule.setBufferSize (myLoop.getStreamObject (), myJoiner.getStreamObject (), BigInteger.valueOf (1));
    }

}
