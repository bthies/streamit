package streamit.scheduler;

import java.util.*;
import java.math.BigInteger;

public class SchedFilter extends SchedStream
{
    Object filter;

    final List srcMsgs = new LinkedList ();
    final List dstMsgs = new LinkedList ();

    public SchedFilter (Object filter, int push, int pop, int peek)
    {
        this.filter = filter;
        setProduction (push);
        setConsumption (pop);
        setPeekConsumption (peek);
    }

    void computeSteadySchedule ()
    {
        // initialize self
        setNumExecutions (BigInteger.ONE);
    }

    public Object getFilter ()
    {
        return filter;
    }
}
