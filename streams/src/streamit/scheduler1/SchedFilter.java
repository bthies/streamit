package streamit.scheduler;

import java.util.*;
import java.math.BigInteger;

public class SchedFilter extends SchedStream
{
    final List srcMsgs = new LinkedList ();
    final List dstMsgs = new LinkedList ();

    public SchedFilter (Object stream, int push, int pop, int peek)
    {
        super (stream);
        setProduction (push);
        setConsumption (pop);
        setPeekConsumption (peek);
    }

    void computeSteadySchedule ()
    {
        // initialize self
        setNumExecutions (BigInteger.ONE);
    }
}
