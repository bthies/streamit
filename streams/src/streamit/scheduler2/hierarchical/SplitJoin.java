package streamit.scheduler.hierarchical;

/* $Id: SplitJoin.java,v 1.1 2002-06-14 15:28:49 karczma Exp $ */

import streamit.scheduler.iriter.SplitJoinIter;
import streamit.scheduler.base.StreamFactory;
import streamit.scheduler.Schedule;

/**
 * This class provides the required functions to implement a schduler
 * for a SplitJOin.  Mostly, it simply implements wrappers for functions
 * in StreamInterface and passes them on to the StreamAlgorithm.  This
 * is necessary, 'cause Java doesn't support multiple inheritance.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

abstract public class SplitJoin
    extends streamit.scheduler.base.SplitJoin
    implements StreamInterface
{
    final protected StreamAlgorithm algorithm = new StreamAlgorithm(this);

    public SplitJoin(SplitJoinIter iterator, StreamFactory factory)
    {
        super(iterator, factory);
    }

    /**
     * compute the initialization and steady state schedules
     */
    abstract public void computeSchedule();

    /**
     * Return an apporpriate hierarchical child.  All children of a 
     * hierarchical splitjoin must be hierarchical as well.  This function
     * asserts if a child is not hierarchical.
     * @return hierarchical child of the splitjoin
     */
    protected StreamInterface getHierarchicalChild(int nChild)
    {
        streamit.scheduler.base.StreamInterface child;
        child = getChild(nChild);
        
        if (!(child instanceof StreamInterface))
        {
            ERROR("This splitjoin contains a child that is not hierarchical");
        }
        
        return (StreamInterface) child;
    }
    
    /**
     * Get the number of phases that the split of this SplitJoin has.
     * @return number of split's phases
     */
    abstract public int getNumSplitPhases();
    
    /**
     * Get the appropriate phase for the split of this SplitJoin.
     * @return phase of the split
     */
    abstract public PhasingSchedule getSplitPhase(int nPhase);

    /**
     * Get the number of phases that the join of this SplitJoin has.
     * @return number of split's join
     */
    abstract public int getNumJoinPhases();

    /**
     * Get the appropriate phase for the join of this SplitJoin.
     * @return phase of the join
     */
    abstract public PhasingSchedule getJoinPhase(int nPhase);

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
        return algorithm.getPhasingInitSchedule ();
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
        return algorithm.getPhasingSteadySchedule ();
    }
}
