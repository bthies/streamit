package streamit.scheduler1.simple;

import streamit.scheduler1.*;

public class SimpleHierarchicalScheduler extends Scheduler
{
    final Schedule schedule;

    protected Schedule getNewSchedule ()
    {
        return new Schedule ();
    }

    public SimpleHierarchicalScheduler ()
    {
        schedule = getNewSchedule ();
    }

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
        ASSERT (stream instanceof SimpleSchedStream);

        SimpleSchedStream simpleStream = (SimpleSchedStream) stream;
        simpleStream.computeSchedule ();

        schedule.setSchedules (simpleStream.getSteadySchedule (), simpleStream.getInitSchedule ());

        return schedule;
    }

    public Schedule getSchedule ()
    {
        return schedule;
    }
}