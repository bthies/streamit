package streamit.scheduler.base;

import streamit.scheduler.iriter./*persistent.*/
FilterIter;

/* $Id: Filter.java,v 1.6 2002-12-02 17:49:36 karczma Exp $ */

/**
 * Computes some basic data for Filters.
 *
 * @version 2
 * @author  Michal Karczmarek
 */

abstract public class Filter extends Stream
{
    final public FilterIter filterIter;

    public Filter(FilterIter _filterIter)
    {
        super (_filterIter.getUnspecializedIter());
        
        ASSERT(_filterIter);
        filterIter = _filterIter;

        computeSteadyState();
    }

    public void computeSteadyState()
    {
        int pop = 0, push = 0;
        int maxPeek = 0;

        // go through all the work functions
        int phase;
        for (phase = 0; phase < filterIter.getNumWorkPhases(); phase++)
        {
            int workPeek = filterIter.getPeekPhase(phase);
            int workPop = filterIter.getPopPhase(phase);
            int workPush = filterIter.getPushPhase(phase);

            // peek will be the maximum of previous peek and current
            // peek - it is possible that previous work function had
            // a peek value that ended up being larger than my peek!
            maxPeek = MAX(maxPeek, pop + workPeek);

            pop += workPop;
            push += workPush;
        }

        setSteadyPeek(maxPeek);
        setSteadyPop(pop);
        setSteadyPush(push);
    }
    
    public int getNumNodes () { return 1; }
    
    public int getNumNodeFirings() { return 1; }
}
