package streamit.scheduler.hierarchical;

/* $Id: FeedbackLoop.java,v 1.2 2002-07-16 01:09:53 karczma Exp $ */

import streamit.scheduler.iriter./*persistent.*/
FeedbackLoopIter;
import streamit.scheduler.base.StreamFactory;
import streamit.scheduler.Schedule;

/**
 * This class provides the required functions to implement a schduler
 * for a FeedbackLoop.  Mostly, it simply implements wrappers for functions
 * in StreamInterface and passes them on to the StreamAlgorithm.  This
 * is necessary, 'cause Java doesn't support multiple inheritance.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

abstract public class FeedbackLoop
    extends streamit.scheduler.base.FeedbackLoop
    implements StreamInterface
{
    final private StreamAlgorithm algorithm = new StreamAlgorithm(this);

    public FeedbackLoop(FeedbackLoopIter iterator, StreamFactory factory)
    {
        super(iterator, factory);
    }

    /**
     * compute the initialization and steady state schedules
     */
    abstract public void computeSchedule();

    /**
     * Returns the hierarchical body of a feedback loop.  This function will
     * assert if the body does not implement hierarchical.StreamInterface.
     * @return the hierarchical body of the feedback loop
     */
    protected StreamInterface getHierarchicalBody()
    {
        if (!(getBody() instanceof StreamInterface))
        {
            ERROR("This feedback loop contains a body that is not hierarchical");
        }

        return (StreamInterface) getBody();
    }

    /**
     * Returns the hierarchical feedback path of a feedback loop.  
     * This function will assert if the body does not implement 
     * hierarchical.StreamInterface.
     * @return the hierarchical feedback path of the feedback loop
     */
    protected StreamInterface getHierarchicalLoop()
    {
        if (!(getLoop() instanceof StreamInterface))
        {
            ERROR("This feedback loop contains a feedback path that is not hierarchical");
        }

        return (StreamInterface) getLoop();
    }

    /**
     * Get the number of phases that the split of this FeedbackLoop has.
     * @return number of split's phases
     */
    public int getNumSplitPhases()
    {
        return feedbackLoop.getSplitterNumWork();
    }

    /**
     * Get the appropriate phase for the split of this FeedbackLoop.
     * @return phase of the split
     */
    public PhasingSchedule getSplitPhase(int nPhase)
    {
        ASSERT(nPhase >= 0 && nPhase < getNumSplitPhases());
        Schedule sched =
            new Schedule(
                feedbackLoop.getSplitterWork(nPhase),
                feedbackLoop.getUnspecializedIter());
        int pushAmount = feedbackLoop.getSplitPushWeights(nPhase)[0];
        return new PhasingSchedule(this, sched, 0, 0, pushAmount);
    }

    /**
     * Get the number of phases that the join of this FeedbackLoop has.
     * @return number of split's join
     */
    public int getNumJoinPhases()
    {
        return feedbackLoop.getJoinerNumWork();
    }

    /**
     * Get the appropriate phase for the join of this FeedbackLoop.
     * @return phase of the join
     */
    public PhasingSchedule getJoinPhase(int nPhase)
    {
        ASSERT(nPhase >= 0 && nPhase < getNumJoinPhases());
        Schedule sched =
            new Schedule(
                feedbackLoop.getJoinerWork(nPhase),
                feedbackLoop.getUnspecializedIter());
        int popAmount = feedbackLoop.getJoinPopWeights(nPhase)[0];
        return new PhasingSchedule(this, sched, popAmount, popAmount, 0);
    }

    public streamit.scheduler.base.StreamInterface getTop()
    {
        return this;
    }

    public streamit.scheduler.base.StreamInterface getBottom()
    {
        return this;
    }

    // These functions implement wrappers for StreamAlgorithm
    // I have to use this stupid style of coding to accomodate
    // Java with its lack of multiple inheritance

    public int getInitPeek()
    {
        return algorithm.getInitPeek();
    }

    public int getInitPop()
    {
        return algorithm.getInitPop();
    }

    public int getInitPush()
    {
        return algorithm.getInitPush();
    }

    public int getNumInitStages()
    {
        return algorithm.getNumInitStages();
    }

    public int getInitStageNumPeek(int stage)
    {
        return algorithm.getInitStageNumPeek(stage);
    }

    public int getInitStageNumPop(int stage)
    {
        return algorithm.getInitStageNumPop(stage);
    }

    public int getInitStageNumPush(int stage)
    {
        return algorithm.getInitStageNumPush(stage);
    }

    public PhasingSchedule getInitScheduleStage(int stage)
    {
        return algorithm.getInitScheduleStage(stage);
    }

    public PhasingSchedule getPhasingInitSchedule()
    {
        return algorithm.getPhasingInitSchedule();
    }

    public Schedule getInitSchedule()
    {
        return algorithm.getInitSchedule();
    }

    public void addInitScheduleStage(PhasingSchedule newStage)
    {
        algorithm.addInitScheduleStage(newStage);
    }

    public int getNumSteadyPhases()
    {
        return algorithm.getNumSteadyPhases();
    }

    public int getSteadyPhaseNumPeek(int phase)
    {
        return algorithm.getSteadyPhaseNumPeek(phase);
    }

    public int getSteadyPhaseNumPop(int phase)
    {
        return algorithm.getSteadyPhaseNumPop(phase);
    }

    public int getSteadyPhaseNumPush(int phase)
    {
        return algorithm.getSteadyPhaseNumPush(phase);
    }

    public PhasingSchedule getSteadySchedulePhase(int phase)
    {
        return algorithm.getSteadySchedulePhase(phase);
    }

    public PhasingSchedule getPhasingSteadySchedule()
    {
        return algorithm.getPhasingSteadySchedule();
    }

    public Schedule getSteadySchedule()
    {
        return algorithm.getSteadySchedule();
    }

    public void addSteadySchedulePhase(PhasingSchedule newPhase)
    {
        algorithm.addSteadySchedulePhase(newPhase);
    }

    public void advanceChildInitSchedule(StreamInterface child)
    {
        algorithm.advanceChildInitSchedule(child, 1);
    }

    public void advanceChildInitSchedule(
        StreamInterface child,
        int numStages)
    {
        algorithm.advanceChildInitSchedule(child, numStages);
    }

    public void advanceChildSteadySchedule(StreamInterface child)
    {
        algorithm.advanceChildSteadySchedule(child, 1);
    }

    public void advanceChildSteadySchedule(
        StreamInterface child,
        int numPhases)
    {
        algorithm.advanceChildSteadySchedule(child, numPhases);
    }

    public PhasingSchedule getChildInitStage(
        StreamInterface child,
        int nStage)
    {
        return algorithm.getChildInitStage(child, nStage);
    }

    public PhasingSchedule getChildNextInitStage(StreamInterface child)
    {
        return algorithm.getChildInitStage(child, 0);
    }
    public PhasingSchedule getChildSteadyPhase(
        StreamInterface child,
        int nPhase)
    {
        return algorithm.getChildSteadyPhase(child, nPhase);
    }

    public PhasingSchedule getChildNextSteadyPhase(StreamInterface child)
    {
        return algorithm.getChildSteadyPhase(child, 0);
    }
}
