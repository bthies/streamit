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

package streamit.scheduler2.hierarchical;

import streamit.scheduler2.Schedule;
import streamit.misc.DestroyedClass;
import java.util.Map;
import java.util.HashMap;

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
        childPhaseShift.put(child, new Integer(shift + numPhases));
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
    
    PhasingSchedule duplicatePhase (PhasingSchedule phase, int numTimes)
    {
        PhasingSchedule duplicatedPhase = new PhasingSchedule (phase.getStream());
        
        final int multiplier = 10;
        for(;numTimes > 0;numTimes = numTimes / multiplier)
        {
            for(int nPhase = numTimes % multiplier;nPhase > 0;nPhase--)
            {
                duplicatedPhase.appendPhase(phase);
            }
            
            PhasingSchedule newSteadyPhase = new PhasingSchedule(phase.getStream());
            for (int nPhase = 0; nPhase < multiplier; nPhase++)
            {
                newSteadyPhase.appendPhase(phase);
            }
            
            phase = newSteadyPhase;
        }
        
        return duplicatedPhase;
    }
    
    /**
     * Append a number of phases of a particular child.
     * This function will make sure that if the child has multiple
     * phases, they will get added appropriately.
     */
    public PhasingSchedule getChildPhases(StreamInterface child, int nPhases)
    {
        PhasingSchedule phase = new PhasingSchedule (child);
        // first make sure that all the phases will be in either
        // steady state or initialization. no cross-over is allowed
        // for simplicity's sake. if cross-over is required, call
        // self recursively
        {
            int childPhaseOffset = getPhaseShift (child);
            if (childPhaseOffset < child.getNumInitStages() && childPhaseOffset + nPhases > child.getNumInitStages())
            {
                // the phases will span across boundary of 
                // initialization and steady state, so break up
                // this computation into two steps - one for 
                // initialization, and one for steady state
                phase.appendPhase(getChildPhases(child, child.getNumInitStages() - childPhaseOffset));
                phase.appendPhase(getChildPhases(child, nPhases - (child.getNumInitStages() - childPhaseOffset)));
                
                return phase;
            }
            
        }
        
        // okay everything is in a single stage - either 
        // steady state or inirialization
        
        // now figure out what the period of child's phases we have
        int nPhasesPeriod;
        {
            if (getPhaseShift(child) < child.getNumInitStages()) nPhasesPeriod = child.getNumInitStages();
            else nPhasesPeriod = child.getNumSteadyPhases();
        }
        
        // figure out how many times we'll go through an entire
        // rotation of the schedule. if less than 4, then just
        // do this in the dumb way 
        if (nPhases / nPhasesPeriod < 4)
        {
            for (int nPhase=0;nPhase < nPhases;nPhase++)
            {
                phase.appendPhase(getChildInitStage(child, 0));
                advanceChildInitSchedule(child, 1);
            }
            return phase;
        }
        
        // okay, I have to do this more than 4 times
        // construct a "smart" schedule. Note that since I'm going to 
        // execute at least 4 rotations of the schedule, I must be
        // in steady state schedule - init schedules can only be
        // executed once through!
        // start with rounding execution up to a steady state
        {
            int mod = nPhases % nPhasesPeriod;
            nPhases -= mod;
            
            for(;mod > 0; mod--)
            {
                phase.appendPhase(getChildSteadyPhase(child, 0));
                advanceChildSteadySchedule(child, 1);
            }
            
        }
        
        // construct a steady state phase
        PhasingSchedule steadyState = new PhasingSchedule (child);
        {
            for (int nPhase = 0; nPhase < child.getNumSteadyPhases(); nPhase++)
            {
                steadyState.appendPhase(getChildSteadyPhase(child, nPhase));
            }
        }
        
        phase.appendPhase(duplicatePhase(steadyState, nPhases/child.getNumSteadyPhases()));
        
        advanceChildSteadySchedule(child, nPhases);
        
        return phase;
    }

    
    /*
    public int getScheduleSize ()
    {
        return getScheduleSize (getSteadySchedule()) + getScheduleSize (getInitializationSchedule ());
    }

    int getScheduleSize (PhasingSchedule sched)
    {
        int size = 0;
        if (sched.getNumPhases() > 1) size += sched.getNumPhases ();
        
        int nPhase;
        for (nPhase = 0; nPhase < sched.getNumPhases(); sched++)
        {
            // compute the size of a phase
            PhasingSchedule phase = sched.getPhase(nPhase);
        }
        
        return size;
    }
    
    Set phasesComputed = new HashSet ();
    
    int getChildScheduleSize (PhasingSchedule sched)
    {
        if (phasesComputed.contains(sched)) return 0;
        return sched.getStream().getScheduleSize (sched);
    }
    */
}
