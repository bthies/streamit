package streamit.scheduler2.minlatency;

/* $Id: Filter.java,v 1.4 2003-05-06 10:23:55 thies Exp $ */

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
    final float phaseFrac;
    public Filter(FilterIter iterator, float _phaseFrac)
    {
        super(iterator);
        phaseFrac = _phaseFrac;
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