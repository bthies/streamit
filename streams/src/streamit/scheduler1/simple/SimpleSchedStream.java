package streamit.scheduler.simple;

import streamit.scheduler.SchedStreamInterface;

interface SimpleSchedStream extends SchedStreamInterface
{
    public void computeSchedule ();
    public Object getSteadySchedule ();
    public Object getInitSchedule ();
}
