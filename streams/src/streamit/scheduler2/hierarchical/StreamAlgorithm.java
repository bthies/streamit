package streamit.scheduler.hierarchical;

/* $Id: StreamAlgorithm.java,v 1.3 2002-07-16 01:09:54 karczma Exp $ */

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
    final private StreamInterface stream;

    final private PhasingSchedule initSchedule;
    final private PhasingSchedule steadySchedule;

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
    public void addSteadySchedulePhase(PhasingSchedule newPhase)
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

    // this section takes care of chilren's schedule phase shifts

    Map childPhaseShift = new HashMap();

    /**
     * Advance the child's init schedule by numStages.
     */
    public void advanceChildInitSchedule(
        StreamInterface child,
        int numStages)
    {
        ASSERT(numStages > 0);

        // get the shift I know about already
        int shift = getPhaseShift(child);

        // store (shift + numStages)
        childPhaseShift.put(child, new Integer(shift + numStages));
    }

    /**
     * Advance the child's steady schedule by numPhases.
     * Makes sure that the child has already executed its full init schedule.
     */
    public void advanceChildSteadySchedule(
        StreamInterface child,
        int numPhases)
    {
        ASSERT(numPhases > 0);

        // get the shift I know about already
        int shift = getPhaseShift(child);

        // make sure that I've gotten out of the init stage!
        ASSERT(shift >= child.getNumInitStages());

        // store shift + 1
        childPhaseShift.put(child, new Integer(shift + 1));
    }

    /**
     * Get a phase/stage shift for a schedule of a child.
     * If the child hasn't been registered as having a phase shift
     * yet, return 0.
     * @return phase/stage shift of a child's schedule
     */
    private int getPhaseShift(StreamInterface child)
    {
        Object shift = childPhaseShift.get(child);
        if (shift == null)
            return 0;
        return ((Integer) shift).intValue();
    }

    /**
     * Get an init stage for a child.  This stage is computed relative
     * to how much of the init schedule has already been consumed.
     * @return init stage of a child
     */
    public PhasingSchedule getChildInitStage(
        StreamInterface child,
        int nStage)
    {
        int realStage = getPhaseShift(child) + nStage;

        if (realStage < child.getNumInitStages())
        {
            // I actually want an init stage
            return child.getInitScheduleStage(realStage);
        }
        else
        {
            // I actually want a steady phase
            int phase =
                (realStage - child.getNumInitStages())
                    % child.getNumSteadyPhases();
            return child.getSteadySchedulePhase(phase);
        }
    }

    /**
     * Get a steady state phase  for a child.  This phase is computed relative
     * to how much of the init and steady schedule has already been consumed.
     * The init schedule must have consumed all of the real init stages already!
     * @return steady state phase of a child
     */
    public PhasingSchedule getChildSteadyPhase(
        StreamInterface child,
        int nPhase)
    {
        int consumedPhases = getPhaseShift(child);

        // the init schedule must have consumed all of the init stages already!
        ASSERT(consumedPhases >= child.getNumInitStages());

        int realPhase = consumedPhases + nPhase - child.getNumInitStages();
        realPhase = realPhase % child.getNumSteadyPhases();

        return child.getSteadySchedulePhase(realPhase);
    }
}
