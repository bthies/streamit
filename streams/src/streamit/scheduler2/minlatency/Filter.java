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
FilterIter;
import streamit.scheduler2.Schedule;
import streamit.scheduler2.hierarchical.PhasingSchedule;

/**
 * This class implements a minimum-latency algorithm for creating
 * schedules.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public class Filter extends streamit.scheduler2.hierarchical.Filter
{
    public Filter(FilterIter iterator)
    {
        super(iterator);
    }

    public void computeSchedule()
    {
        // this is really simple
        // a minimum-latency schedule for a filter consists of just 
        // all the phases and stages for the steady and init schedules

        // do the init schedule
        {
            int nStage;
            for (nStage = 0;
                nStage < filterIter.getNumInitStages();
                nStage++)
            {
                Schedule initCall =
                    new Schedule(
                        filterIter.getInitFunctionStage(nStage),
                        filterIter.getUnspecializedIter());
                PhasingSchedule stage =
                    new PhasingSchedule(
                        this,
                        initCall,
                        filterIter.getInitPeekStage(nStage),
                        filterIter.getInitPopStage(nStage),
                        filterIter.getInitPushStage(nStage));
                addInitScheduleStage(stage);
            }
        }

        // do the steady stage schedule
        {
            int nPhase;
            for (nPhase = 0;
                nPhase < filterIter.getNumWorkPhases();
                nPhase++)
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
                addSteadySchedulePhase(phase);
            }
        }
    }
}
