/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.scheduler2.singleappearance;

import streamit.scheduler2.iriter./*persistent.*/
FeedbackLoopIter;
import streamit.scheduler2.base.StreamFactory;
import streamit.scheduler2.hierarchical.StreamInterface;
import streamit.scheduler2.hierarchical.PhasingSchedule;

/**
 * This class implements a single-appearance algorithm for creating
 * schedules.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public class FeedbackLoop
    extends streamit.scheduler2.hierarchical.FeedbackLoop
{
    PhasingSchedule splitSched, joinSched;

    public FeedbackLoop(FeedbackLoopIter iterator, StreamFactory factory)
    {
        super(iterator, factory);

        // compute the splitter schedule
        {
            splitSched = new PhasingSchedule(this);
            int nPhase;
            for (nPhase = 0; nPhase < super.getNumSplitPhases(); nPhase++)
            {
                splitSched.appendPhase(super.getSplitPhase(nPhase));
            }
        }

        // compute the joiner schedule
        {
            joinSched = new PhasingSchedule(this);
            int nPhase;
            for (nPhase = 0; nPhase < super.getNumJoinPhases(); nPhase++)
            {
                joinSched.appendPhase(super.getJoinPhase(nPhase));
            }
        }
    }

    // Override the functions that deal with schedules for joiner
    // and splitter - single appearance schedules only need one
    // phase for the splitter and joiner schedules respectively

    public int getNumSplitPhases()
    {
        return 1;
    }

    public PhasingSchedule getSplitPhase(int nPhase)
    {
        // single appearance schedule has only one split phase
        ASSERT(nPhase == 0);
        return splitSched;
    }

    /**
     * @return one phase schedule for the splitter
     */
    public PhasingSchedule getSplitPhase()
    {
        return splitSched;
    }

    public int getNumJoinPhases()
    {
        return 1;
    }

    public PhasingSchedule getJoinPhase(int nPhase)
    {
        // single appearance schedule has only one join phase
        ASSERT(nPhase == 0);
        return joinSched;
    }

    /**
     * @return one phase schedule for the joiner
     */
    public PhasingSchedule getJoinPhase()
    {
        return joinSched;
    }

    // this function is basically copied from scheduler v1
    public void computeSchedule()
    {
        StreamInterface body = getHierarchicalBody();
        StreamInterface feedback = getHierarchicalLoop();

        // now initialize all the children appropriately
        {
            body.computeSchedule();
            feedback.computeSchedule();
        }

        // counters for how much data is buffered
        // before each element of the loop
        int bodyBuffer;
        int splitBuffer;
        int loopBuffer;
        int joinBuffer;

        // create steady single appearance schedules for body, loop,
        // these will be used in an the single appearance schedule
        PhasingSchedule steadyBodySched, steadyLoopSched;
        {
            // the body
            {
                steadyBodySched = new PhasingSchedule(body);

                int nPhase;
                for (nPhase = 0;
                    nPhase < body.getNumSteadyPhases();
                    nPhase++)
                {
                    steadyBodySched.appendPhase(
                        body.getSteadySchedulePhase(nPhase));
                }
            }

            // the loop
            {
                steadyLoopSched = new PhasingSchedule(feedback);

                int nPhase;
                for (nPhase = 0;
                    nPhase < feedback.getNumSteadyPhases();
                    nPhase++)
                {
                    steadyLoopSched.appendPhase(
                        feedback.getSteadySchedulePhase(nPhase));
                }
            }
        }

        // compute the initialization schedule
        {
            // create init single phase schedules for body, loop,
            // these will be used in an the single appearance schedule
            PhasingSchedule initBodySched, initLoopSched;
            {
                // the body
                {
                    initBodySched = new PhasingSchedule(body);

                    int nStage;
                    for (nStage = 0;
                        nStage < body.getNumInitStages();
                        nStage++)
                    {
                        initBodySched.appendPhase(
                            body.getInitScheduleStage(nStage));
                    }
                }

                // the loop
                {
                    initLoopSched = new PhasingSchedule(feedback);

                    int nStage;
                    for (nStage = 0;
                        nStage < feedback.getNumInitStages();
                        nStage++)
                    {
                        initLoopSched.appendPhase(
                            feedback.getInitScheduleStage(nStage));
                    }
                }
            }

            int nInitRunSplit;
            int nInitRunBody;
            int nInitRunJoin;

            ASSERT(body);
            ASSERT(feedback);

            // figure out how many times the split needs to run:
            // and initDataProduction for the entire loop
            {
                int feedbackDataNeeded = initLoopSched.getOverallPop();
                feedbackDataNeeded
                    += MAX(
                        (steadyLoopSched.getOverallPeek()
                            - steadyLoopSched.getOverallPop()),
                        (initLoopSched.getOverallPeek()
                            - initLoopSched.getOverallPop()));

                int splitProduction = getSteadySplitFlow().getPushWeight(1);
                nInitRunSplit =
                    (feedbackDataNeeded + splitProduction - 1)
                        / splitProduction;
                loopBuffer =
                    nInitRunSplit * splitProduction
                        - initLoopSched.getOverallPop();
            }

            // figure out how many times the body needs to be run:
            {
                int splitDataNeeded =
                    getSteadySplitFlow().getPopWeight() * nInitRunSplit;
                int bodyProduction = steadyBodySched.getOverallPush();
                nInitRunBody =
                    (splitDataNeeded
                        - initBodySched.getOverallPush()
                        + (bodyProduction - 1))
                        / bodyProduction;
                splitBuffer =
                    nInitRunBody * bodyProduction
                        + initBodySched.getOverallPush()
                        - nInitRunSplit * getSteadySplitFlow().getPopWeight();
            }

            // figure out how many times the join needs to be run
            // and initDataConsumption for the entire loop
            {
                int bodyDataNeeded =
                    initBodySched.getOverallPop()
                        + nInitRunBody * steadyBodySched.getOverallPop();
                bodyDataNeeded
                    += MAX(
                        (steadyBodySched.getOverallPeek()
                            - steadyBodySched.getOverallPop()),
                        (initBodySched.getOverallPeek()
                            - initBodySched.getOverallPop()));

                int joinProduction = getSteadyJoinFlow().getPushWeight();
                nInitRunJoin =
                    (bodyDataNeeded + (joinProduction - 1)) / joinProduction;
                bodyBuffer =
                    nInitRunJoin * joinProduction
                        - initBodySched.getOverallPop()
                        - nInitRunBody * steadyBodySched.getOverallPop();
            }

            // now setup buffer sizes for the join
            {
                int loopInitProduction = initLoopSched.getOverallPush();
                joinBuffer =
                    feedbackLoop.getDelaySize()
                        + loopInitProduction
                        - (nInitRunJoin * getSteadyJoinFlow().getPopWeight(1));

                // check if this is a legal schedule in the first place
                if (feedbackLoop.getDelaySize()
                    < nInitRunJoin * getSteadyJoinFlow().getPopWeight(1))
                {
                    ERROR(
                        "Could not schedule this feedback loop.\n"
                            + "This could because it's impossible to schedule it, \n"
                            + "or because single appearance schedule can't do it");
                }
            }

            // a place-holder schedule necessary to create a 
            // single-appearance schedule
            PhasingSchedule initSchedule = new PhasingSchedule(this);

            // finally, actually create the init schedule
            {
                // add the join schedule
                while (nInitRunJoin > 0)
                {
                    initSchedule.appendPhase(joinSched);
                    nInitRunJoin--;
                }

                // add the body schedule
                initSchedule.appendPhase(initBodySched);
                while (nInitRunBody > 0)
                {
                    initSchedule.appendPhase(steadyBodySched);
                    nInitRunBody--;
                }

                // add the split schedule
                while (nInitRunSplit > 0)
                {
                    initSchedule.appendPhase(splitSched);
                }

                // add the feedback schedule
                initSchedule.appendPhase(initLoopSched);
            }

            // and put it in the real init schedule
            if (initSchedule.getNumPhases() != 0)
                addInitScheduleStage(initSchedule);
        }

        // store the starting buffer sizes:
        int startBodyBuffer = bodyBuffer;
        int startSplitBuffer = splitBuffer;
        int startLoopBuffer = loopBuffer;
        int startJoinBuffer = joinBuffer;

        // counters for how many times each component of the loop gets executed
        int splitExecutions = getNumSplitRounds();
        int joinExecutions = getNumJoinRounds();
        int bodyExecutions = getNumBodyExecs();
        int loopExecutions = getNumLoopExecs();

        // a place-holder schedule necessary to create a single
        // appearance schedule
        PhasingSchedule steadySchedule = new PhasingSchedule(this);

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
                while (joinBuffer >= getSteadyJoinFlow().getPopWeight(1)
                    && joinExecutions > 0)
                {
                    // add the execution to the schedule
                    steadySchedule.appendPhase(joinSched);

                    // move the data forward
                    movedForward = true;
                    joinBuffer -= getSteadyJoinFlow().getPopWeight(1);
                    bodyBuffer += getSteadyJoinFlow().getPushWeight();

                    // check if done, and indicate if so
                    joinExecutions--;
                    if (joinExecutions == 0)
                    {
                        done++;
                    }
                }
            }

            // attempt to push some data through the body of the loop:
            {
                while (bodyBuffer >= steadyBodySched.getOverallPeek()
                    && bodyExecutions > 0)
                {
                    // add the execution to the schedule
                    steadySchedule.appendPhase(steadyBodySched);

                    // move the data forward
                    movedForward = true;
                    bodyBuffer -= steadyBodySched.getOverallPop();
                    splitBuffer += steadyBodySched.getOverallPush();

                    // check if done, and indicate if so
                    bodyExecutions--;
                    if (bodyExecutions == 0)
                    {
                        done++;
                    }
                }
            }
            // attempt to push some data through the split of the loop:
            {
                while (splitBuffer >= getSteadySplitFlow().getPopWeight()
                    && splitExecutions > 0)
                {
                    // add the execution to the schedule
                    steadySchedule.appendPhase(splitSched);

                    // move the data forward
                    movedForward = true;
                    splitBuffer -= getSteadySplitFlow().getPopWeight();
                    loopBuffer += getSteadySplitFlow().getPushWeight(1);

                    // check if done, and indicate if so
                    splitExecutions--;
                    if (splitExecutions == 0)
                    {
                        done++;
                    }
                }
            }

            // attempt to push some data through the feedback path of the loop:
            {
                while (loopBuffer >= steadyLoopSched.getOverallPeek()
                    && loopExecutions > 0)
                {
                    // add the execution to the schedule
                    steadySchedule.appendPhase(steadyLoopSched);

                    // move the data forward
                    movedForward = true;
                    loopBuffer -= steadyLoopSched.getOverallPop();
                    joinBuffer += steadyLoopSched.getOverallPush();

                    // check if done, and indicate if so
                    loopExecutions--;
                    if (loopExecutions == 0)
                    {
                        done++;
                    }
                }
            }

            if (!movedForward)
            {
                ERROR(
                    "Couldn't schedule a feedback loop (if need to find out name\n"
                        + ", ask karczma, and I'll try to add the capability to display it.\n"
                        + "This loop is not necessarily impossible to schedule, "
                        + "but this scheduler isn't intelligent enough to do it");
            }
        }

        // add the steady schedule to the feedback loop's steady schedule
        addSteadySchedulePhase(steadySchedule);

        // make sure that after execution of this schedule, the size of buffers
        // left over is SAME as when we started!
        ASSERT(bodyBuffer == startBodyBuffer);
        ASSERT(splitBuffer == startSplitBuffer);
        ASSERT(loopBuffer == startLoopBuffer);
        ASSERT(joinBuffer == startJoinBuffer);
    }
}
