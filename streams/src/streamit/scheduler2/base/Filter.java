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

import streamit.scheduler2.iriter.FilterIter;
import at.dms.kjc.sir.*;

/**
 * Computes some basic data for Filters.
 * 
 * Namely, it totals up the data consumed and produced by phases
 * of the filter to come up with a steady state total. 
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

        // Debugging:
        if (debugrates) {
            if (librarydebug) {
                System.err.println("FILTER "+ filterIter.getObject().getClass()
                                   .getName());
            } else {
                System.err.println("FILTER "+ ((SIRStream) filterIter.getObject())
                                   .getIdent());
            }
        }
        // End Debugging

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

        // Debugging:
        if (debugrates) {
            if (librarydebug) {
                System.err.print(filterIter.getObject().
                                 getClass().getName());
            } else {
                System.err.print(((SIRStream)filterIter.getObject())
                                 .getIdent()); 
            }   
            System.err.println(" steady state: push " + push + " pop " + pop + " maxPeek " + maxPeek);
        }            
        // End Debugging
    }
    
    public int getNumNodes () { return 1; }
    
    public int getNumNodeFirings() { return 1; }
}
