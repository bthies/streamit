package streamit.scheduler.simple;

import streamit.scheduler.SchedFilter;
import streamit.scheduler.Scheduler;
import java.math.BigInteger;

public class SimpleSchedFilter extends SchedFilter implements SimpleSchedStream
{
    final SimpleHierarchicalScheduler scheduler;

    SimpleSchedFilter (SimpleHierarchicalScheduler scheduler, Object stream, int push, int pop, int peek)
    {
        super (stream, push, pop, peek);
        this.scheduler = scheduler;
    }

    public void computeSchedule ()
    {
        Object userFilter = getStreamObject ();
        ASSERT (userFilter);
    }

    public Object getSteadySchedule ()
    {
        return getStreamObject ();
    }

    public Object getInitSchedule ()
    {
        return null;
    }

    public int getInitDataConsumption ()
    {
        return 0;
    }

    public int getInitDataProduction ()
    {
        return 0;
    }
}
