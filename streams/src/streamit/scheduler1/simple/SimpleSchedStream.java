package streamit.scheduler1.simple;

import streamit.scheduler1.SchedStreamInterface;

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
    public int getInitDataConsumption ();

    /**
     * Return number of data produced by this stream.  Normally this is 0,
     * but feedback loops might need to produce some data.
     */
     public int getInitDataProduction ();
}
