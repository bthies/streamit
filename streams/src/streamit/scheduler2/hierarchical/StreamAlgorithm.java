package streamit.scheduler.hierarchical;

/* $Id: StreamAlgorithm.java,v 1.2 2002-06-13 22:43:29 karczma Exp $ */

import streamit.scheduler.Schedule;
import streamit.misc.DestroyedClass;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;

/**
 * This class provides an implementation for StreamInterface.
 * 
 * I have to make this a separate class instead of a class derrived
 * from Stream and one that Filter will derrive from because
 * Java doesn't have multiple inheritance.  Argh!
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public class StreamAlgorithm extends DestroyedClass
{
    final StreamInterface stream;

    final PhasingSchedule initSchedule;
    final PhasingSchedule steadySchedule;

    StreamAlgorithm(StreamInterface _stream)
    {
        stream = _stream;

        initSchedule = new PhasingSchedule(stream);
        steadySchedule = new PhasingSchedule(stream);
    }

    /**
     * Add a stage to the initialization schedule
     */
    public void addInitScheduleStage(PhasingSchedule newStage)
    {
        initSchedule.appendPhase(newStage);
    }

    /**
     * Add a phase to the steady schedule
     */
    public void addSchedulePhase(PhasingSchedule newPhase)
    {
        steadySchedule.appendPhase(newPhase);
    }

    public int getInitPeek()
    {
        return initSchedule.getOverallPeek();
    }

    public int getInitPop()
    {
        return initSchedule.getOverallPop();
    }

    public int getInitPush()
    {
        return initSchedule.getOverallPush();
    }

    public int getNumInitStages()
    {
        return initSchedule.getNumPhases();
    }

    public int getInitStageNumPeek(int phase)
    {
        return initSchedule.getPhaseNumPeek(phase);
    }

    public int getInitStageNumPop(int phase)
    {
        return initSchedule.getPhaseNumPop(phase);
    }

    public int getInitStageNumPush(int phase)
    {
        return initSchedule.getPhaseNumPush(phase);
    }

    public PhasingSchedule getInitScheduleStage(int phase)
    {
        return initSchedule.getPhase(phase);
    }

    public PhasingSchedule getPhasingInitSchedule()
    {
        return initSchedule;
    }

    public Schedule getInitSchedule()
    {
        return initSchedule.getSchedule();
    }

    public int getNumSteadyPhases()
    {
        return steadySchedule.getNumPhases();
    }

    public int getSteadyPhaseNumPeek(int phase)
    {
        return steadySchedule.getPhaseNumPeek(phase);
    }

    public int getSteadyPhaseNumPop(int phase)
    {
        return steadySchedule.getPhaseNumPop(phase);
    }

    public int getSteadyPhaseNumPush(int phase)
    {
        return steadySchedule.getPhaseNumPush(phase);
    }

    public PhasingSchedule getSteadySchedulePhase(int phase)
    {
        return steadySchedule.getPhase(phase);
    }

    public PhasingSchedule getPhasingSteadySchedule()
    {
        return steadySchedule;
    }

    public Schedule getSteadySchedule()
    {
        return steadySchedule.getSchedule();
    }

    // this section takes care of chilren's steady schedule's phase shifts

    Map childSteadySchedulePhaseShift = new HashMap();

    /**
     * Increment the steady schedule phase shift.
     * This function should be used in order to increment how
     * far into the steady state schedule the init schedule goes.
     */
    public void incrementSteadySchedulePhaseShift(StreamInterface child)
    {
        // get the shift I know about already
        int shift = getSteadySchedulePhaseShift(child);

        // increment it and store it back
        shift = (shift + 1) % child.getNumSteadyPhases();
        childSteadySchedulePhaseShift.put(child, new Integer(shift));
    }

    /**
     * Get a phase shift for a steady schedule of a child.
     * If the child hasn't been registered as having a phase shift
     * yet, return 0.
     * @return phase shift of a child's schedule
     */
    int getSteadySchedulePhaseShift(StreamInterface child)
    {
        Object shift = childSteadySchedulePhaseShift.get(child);
        if (shift == null)
            return 0;
        return ((Integer) shift).intValue();
    }
}
