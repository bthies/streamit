package streamit.scheduler.singleappearance;

/* $Id: Filter.java,v 1.1 2002-06-09 22:38:55 karczma Exp $ */

import streamit.scheduler.iriter.FilterIter;
import streamit.scheduler.Schedule;
import streamit.scheduler.hierarchical.PhasingSchedule;

/**
 * This class implements a single-appearance algorithm for creating
 * schedules.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public class Filter extends streamit.scheduler.hierarchical.Filter
{
    public Filter(FilterIter iterator)
    {
        super(iterator);
    }

    public void computeSchedule()
    {
        PhasingSchedule initPhasingSchedule;
        PhasingSchedule steadyPhasingSchedule;

        // first do the init schedule
        {
            Schedule initSchedule = new Schedule();
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

                initSchedule.addSubSchedule(new Schedule(initFunc));
            }

            initPhasingSchedule =
                new PhasingSchedule(
                    this,
                    initSchedule,
                    initPeek,
                    initPop,
                    initPush);
            algorithm.addInitScheduleStage (initPhasingSchedule);
        }

        // now do the steady schedule
        {
            Schedule steadySchedule = new Schedule();
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

                steadySchedule.addSubSchedule(new Schedule(steadyFunc));
            }

            steadyPhasingSchedule =
                new PhasingSchedule(
                    this,
                    steadySchedule,
                    steadyPeek,
                    steadyPop,
                    steadyPush);
            algorithm.addSchedulePhase (steadyPhasingSchedule);
        }
    }
}
