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

package streamit.scheduler2.singleappearance;

import streamit.scheduler2.iriter./*persistent.*/
FilterIter;
import streamit.scheduler2.Schedule;
import streamit.scheduler2.hierarchical.PhasingSchedule;

/**
 * This class implements a single-appearance algorithm for creating
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
        PhasingSchedule steadyPhasingSchedule;

        // first do the init schedule
        {
            PhasingSchedule initPhasingSchedule = new PhasingSchedule(this);

            Schedule initSchedule = new Schedule(filterIter.getUnspecializedIter());
            int initPeek = 0, initPop = 0, initPush = 0;
            int initStage;
            for (initStage = 0;
                initStage < filterIter.getNumInitStages();
                initStage++)
            {
                Object initFunc = filterIter.getInitFunctionStage(initStage);
                int peek = filterIter.getInitPeekStage(initStage);
                int pop = filterIter.getInitPopStage(initStage);
                int push = filterIter.getInitPushStage(initStage);

                initPeek = MAX(initPeek, initPop + peek);
                initPop += pop;
                initPush += push;

                initPhasingSchedule.appendPhase(
                    new PhasingSchedule(
                        this,
                        new Schedule(
                            initFunc,
                            filterIter.getUnspecializedIter()),
                        peek,
                        pop,
                        push));
            }

            if (initPhasingSchedule.getNumPhases() != 0)
                addInitScheduleStage(initPhasingSchedule);
        }

        // now do the steady schedule
        {
            Schedule steadySchedule = new Schedule(filterIter.getUnspecializedIter());
            int steadyPeek = 0, steadyPop = 0, steadyPush = 0;
            int steadyPhase;
            for (steadyPhase = 0;
                steadyPhase < filterIter.getNumWorkPhases();
                steadyPhase++)
            {
                Object steadyFunc =
                    filterIter.getWorkFunctionPhase(steadyPhase);
                int peek = filterIter.getPeekPhase(steadyPhase);
                int pop = filterIter.getPopPhase(steadyPhase);
                int push = filterIter.getPushPhase(steadyPhase);

                steadyPeek = MAX(steadyPeek, steadyPop + peek);
                steadyPop += pop;
                steadyPush += push;

                steadySchedule.addSubSchedule(
                    new Schedule(
                        steadyFunc,
                        filterIter.getUnspecializedIter()));
            }

            steadyPhasingSchedule =
                new PhasingSchedule(
                    this,
                    steadySchedule,
                    steadyPeek,
                    steadyPop,
                    steadyPush);
            addSteadySchedulePhase(steadyPhasingSchedule);
        }
    }
}
