package streamit.scheduler.base;

import streamit.scheduler.iriter.FilterIter;

/* $Id: Filter.java,v 1.1 2002-05-27 03:18:49 karczma Exp $ */

/**
 * Computes some basic data for Filters.
 *
 * @version 2
 * @author  Michal Karczmarek
 */

public class Filter extends Stream
{
    FilterIter filter;

    Filter(FilterIter _filter)
    {
        ASSERT(_filter);
        filter = _filter;
    }

    public void computeSteadyState()
    {
        // not tested yet.
        ASSERT (false);
        
        int pop = 0, push = 0;
        int maxPeek = 0;

        // go through all the work functions
        int phase;
        for (phase = 0; phase < filter.getNumWorkPhases(); phase++)
        {
            int workPeek = filter.getPeekPhase(phase);
            int workPop = filter.getPopPhase(phase);
            int workPush = filter.getPushPhase(phase);

            // peek will be the maximum of previous peek and current
            // peek - it is possible that previous work function had
            // a peek value that ended up being larger than my peek!
            maxPeek = MAX (maxPeek, pop + workPeek);

            pop += workPop;
            push += workPush;
        }

        setSteadyPeek(maxPeek);
        setSteadyPop(pop);
        setSteadyPush(push);
    }
}
