package streamit.scheduler.simple;

import streamit.scheduler.*;
import java.util.*;
import java.math.BigInteger;

public class SimpleHierarchicalScheduler extends Scheduler
{
    final Schedule schedule = new Schedule ();

    /**
     * Construct a new filter object
     */
    public SchedFilter newSchedFilter (Object stream, int push, int pop, int peek)
    {
        return new SimpleSchedFilter (this, stream, push, pop, peek);
    }

    /**
     * Construct a new pipeline object
     */
    public SchedPipeline newSchedPipeline (Object stream)
    {
        return new SimpleSchedPipeline (this, stream);
    }

    /**
     * Construct a new splitjoin object
     */
    public SchedSplitJoin newSchedSplitJoin (Object stream)
    {
        return new SimpleSchedSplitJoin (this, stream);
    }

    /**
     * Construct a new feedback loop object
     */
    public SchedLoop newSchedLoop (Object stream, SchedJoinType join, SchedStream body, SchedSplitType split, SchedStream loop, int delay)
    {
        return new SimpleSchedLoop (this, stream, join, body, split, loop, delay);
    }

    public Schedule computeSchedule ()
    {
        ASSERT (stream);

        Object schedule;
        schedule = computeSchedule (stream);
        ASSERT (schedule);

        this.schedule.setSchedule (schedule);

        return this.schedule;
    }

    final Map subSchedules = new HashMap ();

    public void setBufferSize (Object streamSrc, Object streamDst, BigInteger bufferSize)
    {
        schedule.setBufferSize (streamSrc, streamDst, bufferSize);
    }

    Object computeSchedule (SchedStream stream)
    {
        Object schedule;

        if (subSchedules.containsKey (stream))
        {
            // just retrieve the schedule and discard the list given to me
            schedule = subSchedules.get (stream);
            ASSERT (schedule);
            return schedule;
        } else {
            // have to compute it:
            ASSERT (stream instanceof SimpleSchedStream);
            schedule = ((SimpleSchedStream)stream).computeSchedule ();

            ASSERT (schedule);
            subSchedules.put (stream, schedule);
        }

        return schedule;
    }
}