package streamit.scheduler2.hierarchical;

/* $Id: Pipeline.java,v 1.7 2003-05-06 10:23:50 thies Exp $ */

import streamit.scheduler2.iriter./*persistent.*/
PipelineIter;
import streamit.scheduler2.base.StreamFactory;
import streamit.scheduler2.Schedule;

/**
 * This class provides the required functions to implement a schduler
 * for a Pipeline.  Mostly, it simply implements wrappers for functions
 * in StreamInterface and passes them on to the StreamAlgorithm.  This
 * is necessary, 'cause Java doesn't support multiple inheritance.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

abstract public class Pipeline
    extends streamit.scheduler2.base.Pipeline
    implements StreamInterface
{
    final private StreamAlgorithm algorithm = new StreamAlgorithm(this);

    public Pipeline(PipelineIter iterator, StreamFactory factory)
    {
        super(iterator, factory);
    }

    /**
     * compute the initialization and steady state schedules
     */
    abstract public void computeSchedule();

    /**
     * Return an apporpriate hierarchical child.  All children of a 
     * hierarchical pipeline must be hierarchical as well.  This function
     * asserts if a child is not hierarchical.
     * @return hierarchical child of the pipeline
     */
    protected StreamInterface getHierarchicalChild(int nChild)
    {
        streamit.scheduler2.base.StreamInterface child;
        child = getChild(nChild);

        if (!(child instanceof StreamInterface))
        {
            ERROR("This pipeline contains a child that is not hierarchical");
        }

        return (StreamInterface) child;
    }

    public streamit.scheduler2.base.StreamInterface getTop()
    {
        return getChild(0);
    }

    public streamit.scheduler2.base.StreamInterface getBottom()
    {
        return getChild(getNumChildren() - 1);
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
    
    public PhasingSchedule getChildPhases(StreamInterface child, int nPhases)
    {
        return algorithm.getChildPhases(child, nPhases);
    }
}
