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

import java.util.Vector;
import streamit.scheduler2.Schedule;
import streamit.misc.DestroyedClass;

/**
 * This class stores all information about PhasingSchedules.
 * Phasing schedules basically are schedules that have sub-schedules.
 * Here, a phasing schedule will either have multiple sub-schedules
 * (sub phasing-schedules), or it will have a single Schedule.
 * 
 * Due to lack of multiple inheritance in Java, I am forced to 
 * implement new functionality by splitting each new object
 * into an algorithm and "shell" implementation.  Algorithms
 * provide the common functionality for new object.  "shell" object
 * simply call algorithm objects to perform appropriate operations.
 * This is silly, and is a consequence of Java constraining the
 * programmer so s/he can't shoot him self in the foot.  Good
 * programmers feel handcuffed, and bad programmers continue coming
 * up with new creative ways to shoot themselves in the foot.
 */

public class PhasingSchedule extends DestroyedClass
{
    int nPhases;
    int overallPeek, overallPop, overallPush;
    final Vector peekSize = new Vector();
    final Vector popSize = new Vector();
    final Vector pushSize = new Vector();
    final Vector phases = new Vector();
    Schedule phasingPreComputedSchedule;
    Schedule schedule;
    final StreamInterface stream;

    /**
     * Setup a PhasingSchedule with a real Schedule.
     */
    public PhasingSchedule(
        StreamInterface _stream,
        Schedule _schedule,
        int peekAmount,
        int popAmount,
        int pushAmount)
    {
        stream = _stream;
        schedule = _schedule;
        nPhases = 1;
        peekSize.add(new Integer(peekAmount));
        popSize.add(new Integer(popAmount));
        pushSize.add(new Integer(pushAmount));
        overallPeek = peekAmount;
        overallPop = popAmount;
        overallPush = pushAmount;
    }

    /**
     * Setup a PhasingSchedule for being a proper phasing schedule.
     */
    public PhasingSchedule(StreamInterface _stream)
    {
        stream = _stream;
        overallPeek = 0;
        overallPop = 0;
        overallPush = 0;
    }

    /**
     * Get the stream that corresponds to this particular phase.
     * @return phase's stream
     */
    StreamInterface getStream()
    {
        return stream;
    }

    /**
     * get the number of phases in this schedule
     * @return number of phases in this schedule
     */
    public int getNumPhases()
    {
        return nPhases;
    }

    /**
     * get the number of data peeked by the appropriate phase
     * @return number of data peeked by phase
     */
    public int getPhaseNumPeek(int phase)
    {
        // phase must be within range
        ASSERT(phase >= 0 && phase < nPhases);
        return ((Integer) peekSize.get(phase)).intValue();
    }

    /**
     * get the number of data popped by the appropriate phase
     * @return number of data popped by phase
     */
    public int getPhaseNumPop(int phase)
    {
        // phase must be within range
        ASSERT(phase >= 0 && phase < nPhases);
        return ((Integer) popSize.get(phase)).intValue();
    }

    /**
     * get the number of data pushed by the appropriate phase
     * @return number of data pushed by phase
     */
    public int getPhaseNumPush(int phase)
    {
        // phase must be within range
        ASSERT(phase >= 0 && phase < nPhases);
        return ((Integer) pushSize.get(phase)).intValue();
    }

    /**
     * get a particular phase of the schedule
     * @return phase of the schedule
     */
    public PhasingSchedule getPhase(int phase)
    {
        // phase must be within range
        ASSERT(phase >= 0 && phase < nPhases);

        // and this must be a phasing schedule
        ASSERT(phases != null);

        return (PhasingSchedule) phases.get(phase);
    }

    /**
     * Append a phase to this phasing schedule.  If the sub-stream
     * being appended is either the top or the bottom of this
     * stream, this function will store its appropriate push/pop/peek.
     * Otherwise, it will store 0's, as this sub-stream does not
     * affect communication outside this stream.
     */
    public void appendPhase(PhasingSchedule phase)
    {
        // invalidate the old pre-computed schedule
        phasingPreComputedSchedule = null;

        // append the new phase
        nPhases++;
        phases.add(phase);

        // find out how much this phase communicates withoutside world
        int peek = 0, pop = 0, push = 0;
        StreamInterface phaseStream = phase.getStream();

        // if the stream corresponding to this phase is my stream
        // (somebody constructed a phase for my stream specifically)
        // or if it is the top of my stream, I need to account for this
        // phase's peek & pop in my peek & pop
        if (phaseStream == stream || phaseStream == stream.getTop())
        {
            peek = phase.getOverallPeek();
            pop = phase.getOverallPop();
        }

        // if the stream corresponding to this phase is my stream
        // (somebody constructed a phase for my stream specifically)
        // or if it is the top of my stream, I need to account for this
        // phase's push in my push
        if (phaseStream == stream || phaseStream == stream.getBottom())
        {
            push = phase.getOverallPush();
        }

        // update this stream's communication
        overallPeek = MAX(overallPeek, overallPop + peek);
        overallPop = overallPop + pop;
        overallPush = overallPush + push;
    }
    
    /**
     * Get the amount of data that this phasing schedule peeks, when
     * all of its phases are executed.  This amount is reported as
     * seen by the object whose schedule is being reported here.
     * @return peek size for overall schedule
     */
    public int getOverallPeek()
    {
        return overallPeek;
    }

    /**
     * Get the amount of data that this phasing schedule pops, when
     * all of its phases are executed.  This amount is reported as
     * seen by the object whose schedule is being reported here.
     * @return pop size for overall schedule
     */
    public int getOverallPop()
    {
        return overallPop;
    }

    /**
     * Get the amount of data that this phasing schedule pushes, when
     * all of its phases are executed.  This amount is reported as
     * seen by the object whose schedule is being reported here.
     * @return push size for overall schedule
     */
    public int getOverallPush()
    {
        return overallPush;
    }

    /**
     * get a Schedule that corresponds to this phasing schedule
     * @return corresponding Schedule
     */
    Schedule getSchedule()
    {
        // is this phasing schedule just a wrapper for a real schedule?
        if (schedule != null)
        {
            // yes: return the schedule and be happy
            return schedule;
        }
        else
        {
            // do I have a pre-computed version?
            // if I do, just return it!
            if (phasingPreComputedSchedule != null)
                return phasingPreComputedSchedule;

            // now: create a schedule out of my phasing schedule...
            Schedule sched = new Schedule(getStream().getStreamIter());
            int phase;
            for (phase = 0; phase < getNumPhases(); phase++)
            {
                sched.addSubSchedule(getPhase(phase).getSchedule());
            }

            phasingPreComputedSchedule = sched;


            return sched;
        }
    }
}
