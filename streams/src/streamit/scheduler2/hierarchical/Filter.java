package streamit.scheduler.hierarchical;

/* $Id: Filter.java,v 1.1 2002-06-09 22:38:51 karczma Exp $ */

import streamit.scheduler.iriter.FilterIter;
import streamit.scheduler.Schedule;

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
    extends streamit.scheduler.base.Filter
    implements StreamInterface
{
    final protected StreamAlgorithm algorithm = new StreamAlgorithm(this);

    public Filter(FilterIter iterator)
    {
        super(iterator);
    }

    /**
     * compute the initialization and steady state schedules
     */
    abstract public void computeSchedule();
    
    public StreamInterface getTop ()
    {
        return this;
    }
    
    public StreamInterface getBottom ()
    {
        return this;
    }

    // These functions implement wrappers for StreamAlgorithm
    // I have to use this stupid style of coding to accomodate
    // Java with its lack of multiple inheritance

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

    public Schedule getInitSchedule()
    {
        return algorithm.getInitSchedule();
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

    public Schedule getSteadySchedule()
    {
        return algorithm.getSteadySchedule();
    }
}
