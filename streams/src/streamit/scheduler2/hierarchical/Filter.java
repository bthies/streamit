package streamit.scheduler2.hierarchical;

import streamit.scheduler2.iriter./*persistent.*/
FilterIter;
import streamit.scheduler2.Schedule;

/**
 * This class provides the required functions to implement a schduler
 * for a Filter.  Mostly, it simply implements wrappers for functions
 * in StreamInterface and passes them on to the StreamAlgorithm.  This
 * is necessary, 'cause Java doesn't support multiple inheritance.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

abstract public class Filter
    extends streamit.scheduler2.base.Filter
    implements StreamInterface
{
    final private StreamAlgorithm algorithm = new StreamAlgorithm(this);

    public Filter(FilterIter iterator)
    {
        super(iterator);
    }

    /**
     * compute the initialization and steady state schedules
     */
    abstract public void computeSchedule();

    public streamit.scheduler2.base.StreamInterface getTop()
    {
        return this;
    }

    public streamit.scheduler2.base.StreamInterface getBottom()
    {
        return this;
    }

    public int getNumDeclaredSteadyPhases()
    {
        return filterIter.getNumWorkPhases();
    }

    public int getDeclaredSteadyPhasePeek(int nPhase)
    {
        return filterIter.getPeekPhase(nPhase);
    }

    public int getDeclaredSteadyPhasePop(int nPhase)
    {
        return filterIter.getPopPhase(nPhase);
    }

    public int getDeclaredSteadyPhasePush(int nPhase)
    {
        return filterIter.getPushPhase(nPhase);
    }

    public PhasingSchedule getDeclaredSteadyPhase(int nPhase)
    {
        Schedule workFunction =
            new Schedule(
                filterIter.getWorkFunctionPhase(nPhase),
                filterIter.getUnspecializedIter());
        PhasingSchedule phase =
            new PhasingSchedule(
                this,
                workFunction,
                filterIter.getPeekPhase(nPhase),
                filterIter.getPopPhase(nPhase),
                filterIter.getPushPhase(nPhase));
        return phase;
    }

    public int getNumDeclaredInitPhases()
    {
        return filterIter.getNumInitStages();
    }

    public int getDeclaredInitPhasePeek(int nPhase)
    {
        return filterIter.getInitPeekStage(nPhase);
    }

    public int getDeclaredInitPhasePop(int nPhase)
    {
        return filterIter.getInitPopStage(nPhase);
    }

    public int getDeclaredInitPhasePush(int nPhase)
    {
        return filterIter.getInitPushStage(nPhase);
    }

    public PhasingSchedule getDeclaredInitPhase(int nPhase)
    {
        Schedule initCall =
            new Schedule(
                filterIter.getInitFunctionStage(nPhase),
                filterIter.getUnspecializedIter());
        PhasingSchedule initPhase =
            new PhasingSchedule(
                this,
                initCall,
                filterIter.getInitPeekStage(nPhase),
                filterIter.getInitPopStage(nPhase),
                filterIter.getInitPushStage(nPhase));
        return initPhase;
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
