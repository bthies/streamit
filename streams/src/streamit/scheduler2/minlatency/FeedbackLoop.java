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

package streamit.scheduler2.minlatency;

import streamit.scheduler2.iriter./*persistent.*/
FeedbackLoopIter;
import streamit.scheduler2.hierarchical.StreamInterface;
import streamit.scheduler2.base.StreamFactory;
import streamit.scheduler2.hierarchical.PhasingSchedule;

/**
 * This class implements a minimum-latency algorithm for creating
 * schedules.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public class FeedbackLoop
    extends streamit.scheduler2.hierarchical.FeedbackLoop
{
    public FeedbackLoop(FeedbackLoopIter iterator, StreamFactory factory)
    {
        super(iterator, factory);
    }

    private interface FLoopSchedulingUtility
    {
        public void addSchedulePhase(PhasingSchedule phase);
        public void advanceChildSchedule(StreamInterface child);
        public PhasingSchedule getChildNextPhase(StreamInterface child);
        public PhasingSchedule getChildPhase(
            StreamInterface child,
            int phase);
        public void advanceSplitSchedule();
        public void advanceJoinSchedule();

        public SplitFlow getNextSplitSteadyPhaseFlow();
        public SplitFlow getSplitSteadyPhaseFlow(int phase);
        public PhasingSchedule getNextSplitSteadyPhase();
        public JoinFlow getNextJoinSteadyPhaseFlow();
        public JoinFlow getJoinSteadyPhaseFlow(int phase);
        public PhasingSchedule getNextJoinSteadyPhase();
    }

    private class FLoopInitSchedulingUtility
        implements FLoopSchedulingUtility
    {
        FeedbackLoop floop;
        FLoopInitSchedulingUtility(FeedbackLoop _floop)
        {
            floop = _floop;
        }

        public void addSchedulePhase(PhasingSchedule phase)
        {
            floop.addInitScheduleStage(phase);
        }

        public void advanceChildSchedule(StreamInterface child)
        {
            floop.advanceChildInitSchedule(child);
        }

        public PhasingSchedule getChildNextPhase(StreamInterface child)
        {
            return floop.getChildNextInitStage(child);
        }

        public PhasingSchedule getChildPhase(
            StreamInterface child,
            int stage)
        {
            return floop.getChildInitStage(child, stage);
        }

        public void advanceSplitSchedule()
        {
            floop.advanceSplitSchedule();
        }

        public void advanceJoinSchedule()
        {
            floop.advanceJoinSchedule();
        }

        public SplitFlow getNextSplitSteadyPhaseFlow()
        {
            return floop.getNextSplitSteadyPhaseFlow();
        }

        public SplitFlow getSplitSteadyPhaseFlow(int phase)
        {
            return floop.getSplitSteadyPhaseFlow(phase);
        }

        public PhasingSchedule getNextSplitSteadyPhase()
        {
            return floop.getNextSplitSteadyPhase();
        }

        public JoinFlow getNextJoinSteadyPhaseFlow()
        {
            return floop.getNextJoinSteadyPhaseFlow();
        }

        public JoinFlow getJoinSteadyPhaseFlow(int phase)
        {
            return floop.getJoinSteadyPhaseFlow(phase);
        }

        public PhasingSchedule getNextJoinSteadyPhase()
        {
            return floop.getNextJoinSteadyPhase();
        }
    }

    private class FLoopSteadySchedulingUtility
        implements FLoopSchedulingUtility
    {
        FeedbackLoop floop;
        FLoopSteadySchedulingUtility(FeedbackLoop _floop)
        {
            floop = _floop;
        }

        public void addSchedulePhase(PhasingSchedule phase)
        {
            floop.addSteadySchedulePhase(phase);
        }

        public void advanceChildSchedule(StreamInterface child)
        {
            floop.advanceChildSteadySchedule(child);
        }

        public PhasingSchedule getChildNextPhase(StreamInterface child)
        {
            return floop.getChildNextSteadyPhase(child);
        }

        public PhasingSchedule getChildPhase(
            StreamInterface child,
            int stage)
        {
            return floop.getChildSteadyPhase(child, stage);
        }

        public void advanceSplitSchedule()
        {
            floop.advanceSplitSchedule();
        }

        public void advanceJoinSchedule()
        {
            floop.advanceJoinSchedule();
        }

        public SplitFlow getNextSplitSteadyPhaseFlow()
        {
            return floop.getNextSplitSteadyPhaseFlow();
        }

        public SplitFlow getSplitSteadyPhaseFlow(int phase)
        {
            return floop.getSplitSteadyPhaseFlow(phase);
        }

        public PhasingSchedule getNextSplitSteadyPhase()
        {
            return floop.getNextSplitSteadyPhase();
        }

        public JoinFlow getNextJoinSteadyPhaseFlow()
        {
            return floop.getNextJoinSteadyPhaseFlow();
        }

        public JoinFlow getJoinSteadyPhaseFlow(int phase)
        {
            return floop.getJoinSteadyPhaseFlow(phase);
        }

        public PhasingSchedule getNextJoinSteadyPhase()
        {
            return floop.getNextJoinSteadyPhase();
        }
    }

    public void computeMinLatencySchedule(
        FLoopSchedulingUtility utility,
        int splitExecs,
        int joinExecs,
        int bodyExecs,
        int loopExecs,
        int preBodyBuffer[],
        int postBodyBuffer[],
        int preLoopBuffer[],
        int postLoopBuffer[])
    {
        StreamInterface body = getHierarchicalBody();
        StreamInterface feedback = getHierarchicalLoop();

        // the original phase.  More will be created as needed
        PhasingSchedule phase = new PhasingSchedule(this);

        // basically, I will want to pull data from the loop as long as
        // some of the elements still need to get executed:
        // this loop will simply attempt moving some data forward.
        // if this can be accomplished, it'll "continue" and see if
        // some data can be pushed out.  This'll guarantee a proper pull
        // schedule.
        while (splitExecs > 0
            || joinExecs > 0
            || bodyExecs > 0
            || loopExecs > 0)
        {
            // try to move some data through the splitter:
            if (splitExecs > 0)
            {
                // figure out if I can run a phase of the splitter
                SplitFlow splitFlow = utility.getNextSplitSteadyPhaseFlow();
                if (splitFlow.getPopWeight() <= postBodyBuffer[0])
                {
                    // yes - add it to the schedule, update buffers
                    // and, if phase outputs data, complete it 
                    // and start a new phase
                    splitExecs--;
                    phase.appendPhase(utility.getNextSplitSteadyPhase());
                    utility.advanceSplitSchedule();
                    postBodyBuffer[0] -= splitFlow.getPopWeight();
                    preLoopBuffer[0] += splitFlow.getPushWeight(1);
                    if (splitFlow.getPushWeight(0) > 0)
                    {
                        utility.addSchedulePhase(phase);
                        phase = new PhasingSchedule(this);
                    }
                    continue;
                }

                // Okay, I don't have enough data to execute this phase
                // of the split.  Check if the body will attempt to execute.
                // If it will attempt, just fall through and let it try.
                // If it won't (out of needed phases), force it to execute
                // by asking it for another phase
                if (bodyExecs == 0)
                {
                    bodyExecs = 1;
                }
            }

            // try to move some data through the body
            if (bodyExecs > 0)
            {
                // figure out if I can run a phase of the body
                PhasingSchedule bodyPhase = utility.getChildNextPhase(body);
                if (bodyPhase.getOverallPeek() <= preBodyBuffer[0])
                {
                    // yes - add this phase to the schedule, update buffers
                    // and continue from the top
                    bodyExecs--;
                    phase.appendPhase(bodyPhase);
                    utility.advanceChildSchedule(body);
                    preBodyBuffer[0] -= bodyPhase.getOverallPop();
                    postBodyBuffer[0] += bodyPhase.getOverallPush();
                    continue;
                }

                // I don't have enough data to advance the body
                // if the joiner isn't planned to execute quite yet
                // then I better force it to execute
                if (joinExecs == 0)
                {
                    joinExecs = 1;
                }
            }

            // try to move some data through the joiner:
            if (joinExecs > 0)
            {
                // figure out if I can run a phase of the joiner
                JoinFlow joinFlow = utility.getJoinSteadyPhaseFlow(0);
                if (joinFlow.getPopWeight(1) <= postLoopBuffer[0])
                {
                    // yes - add it to the schedule, update buffers
                    // and continue
                    joinExecs--;
                    phase.appendPhase(utility.getNextJoinSteadyPhase());
                    utility.advanceJoinSchedule();
                    postLoopBuffer[0] -= joinFlow.getPopWeight(1);
                    preBodyBuffer[0] += joinFlow.getPushWeight();
                    continue;
                }

                // Okay, I don't have enough data to execute this phase
                // of the join.  Check if the loop will attempt to execute.
                // If it will attempt, just fall through and let it try.
                // If it won't (out of needed phases), force it to execute
                // by asking it for another phase
                if (loopExecs == 0)
                {
                    loopExecs = 1;
                }
            }

            // try to move some data through the loop
            if (loopExecs > 0)
            {
                // figure out if I can run a phase of the body
                PhasingSchedule loopPhase =
                    utility.getChildNextPhase(feedback);
                if (loopPhase.getOverallPeek() <= preLoopBuffer[0])
                {
                    // yes - add this phase to the schedule, update buffers
                    // and continue from the top
                    loopExecs--;
                    phase.appendPhase(loopPhase);
                    utility.advanceChildSchedule(feedback);
                    preLoopBuffer[0] -= loopPhase.getOverallPop();
                    postLoopBuffer[0] += loopPhase.getOverallPush();
                    continue;
                }

                // I don't have enough data to advance the loop.
                // Tough shit.  I cannot schedule this loop.  If the
                // children can get a more fine-grained schedule, then 
                // I may be able to schedule it if their schedules are
                // relaxed.  But if not, I cannot do ANYTHING with this
                // loop, as it is not possible to schedule it!
            }

            ERROR(
                "Couldn't schedule a feedback loop (if need to find out name,\n"
                    + "ask karczma, and I'll try to add the capability to display it.\n"
                    + "This loop is not necessarily impossible to schedule, \n"
                    + "but the children's schedules may need to be more fine-grained\n");
        }

        // okay, it is possible that I just added stuff to the phase, but
        // it didn't get automagically added to the real schedule
        // if the phase is not empty, add it
        if (phase.getNumPhases() != 0)
        {
            utility.addSchedulePhase(phase);
        }
    }

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
        int preBodyBuffer[] = new int[1];
        int postBodyBuffer[] = new int[1];
        int preLoopBuffer[] = new int[1];
        int postLoopBuffer[] = new int[1];
        
        // the post feedback buffer gets intitialized with some data:
        postLoopBuffer [0] = feedbackLoop.getDelaySize();

        // compute how many times each subcomponent needs to be run
        // during initialization (these are minimums), and use that
        // information to create a minimum latency, pull intialization
        // schedule
        {
            int numInitBodyExec = body.getNumInitStages();
            int numInitLoopExec = feedback.getNumInitStages();
            int numInitJoinExec = 0;
            int numInitSplitExec = 0;

            // figure out how many times I need to run the joiner
            // in order to satisfy the body's needs for data 
            // in steady state
            {
                int bodyInitPop = body.getInitPop();
                int bodyInitPeek = body.getInitPeek();
                int bodySteadyPop = body.getSteadyPop();
                int bodySteadyPeek = body.getSteadyPeek();

                int bodyNeedData =
                    bodyInitPop
                        + MAX(
                            bodyInitPeek - bodyInitPop,
                            bodySteadyPeek - bodySteadyPop);

                while (bodyNeedData > 0)
                {
                    JoinFlow joinPhase =
                        getJoinSteadyPhaseFlow(numInitJoinExec);
                    numInitJoinExec++;

                    bodyNeedData -= joinPhase.getPushWeight();
                }
            }

            // figure out how many times I need to run the splitter
            // in order to satisfy the feedback's needs for data 
            // in steady state
            {
                int loopInitPop = feedback.getInitPop();
                int loopInitPeek = feedback.getInitPeek();
                int loopSteadyPop = feedback.getSteadyPop();
                int loopSteadyPeek = feedback.getSteadyPeek();

                int loopNeedData =
                    loopInitPop
                        + MAX(
                            loopInitPeek - loopInitPop,
                            loopSteadyPeek - loopSteadyPop);

                while (loopNeedData > 0)
                {
                    SplitFlow splitPhase =
                        getSplitSteadyPhaseFlow(numInitSplitExec);
                    numInitSplitExec++;

                    loopNeedData -= splitPhase.getPushWeight(1);
                }
            }

            // and magically compute the schedule!
            computeMinLatencySchedule(
                new FLoopInitSchedulingUtility(this),
                numInitSplitExec,
                numInitJoinExec,
                numInitBodyExec,
                numInitLoopExec,
                preBodyBuffer,
                postBodyBuffer,
                preLoopBuffer,
                postLoopBuffer);
        }

        // and magically compute the steady schedule:
        {
            int numSplitExec = getNumSplitRounds() * getNumSplitPhases();
            int numJoinExec = getNumJoinRounds() * getNumJoinPhases();
            int numBodyExec = getNumBodyExecs() * body.getNumSteadyPhases();
            int numLoopExec =
                getNumLoopExecs() * feedback.getNumSteadyPhases();

            computeMinLatencySchedule(
                new FLoopSteadySchedulingUtility(this),
                numSplitExec,
                numJoinExec,
                numBodyExec,
                numLoopExec,
                preBodyBuffer,
                postBodyBuffer,
                preLoopBuffer,
                postLoopBuffer);
        }

        // done!
    }

}
