package streamit.scheduler.simple;

import streamit.scheduler.SchedFilter;
import streamit.scheduler.Scheduler;

public class SimpleSchedFilter extends SchedFilter implements SimpleSchedStream
{
    final SimpleHierarchicalScheduler scheduler;

    SimpleSchedFilter (SimpleHierarchicalScheduler scheduler, Object stream, int push, int pop, int peek)
    {
        super (stream, push, pop, peek);
        this.scheduler = scheduler;
    }

    public Object computeSchedule ()
    {
        Object userFilter = getStreamObject ();
        ASSERT (userFilter);

        return userFilter;
    }
}
