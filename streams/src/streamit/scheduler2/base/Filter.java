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

package streamit.scheduler2.base;

import streamit.scheduler2.iriter./*persistent.*/
FilterIter;

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
        
        assert _filterIter != null;
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
