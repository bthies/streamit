package streamit.scheduler;

import java.util.*;
import java.math.BigInteger;

public class SchedFilter extends SchedStream
{
    Object operator;

    final List srcMsgs = new LinkedList ();
    final List dstMsgs = new LinkedList ();

    void computeSchedule ()
    {
        // initialize self
        numExecutions = BigInteger.ONE;
    }
}
