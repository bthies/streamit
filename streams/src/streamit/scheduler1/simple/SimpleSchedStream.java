package streamit.scheduler.simple;

import streamit.scheduler.SchedStreamInterface;

interface SimpleSchedStream extends SchedStreamInterface
{
    public void computeSchedule ();
    public Object getSteadySchedule ();
    public Object getInitSchedule ();

    /**
     * This will return number of data that this stream consumes in order to
     * initialize itself.
     * This DOES NOT include the (peek - pop) amount!
     */
    public int getInitDataCount ();
}
