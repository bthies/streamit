package streamit.scheduler;

import java.util.*;
import java.math.BigInteger;

import streamit.AssertedClass;

public class Schedule extends AssertedClass
{
    /**
     * This map holds size of buffers needed between various streams
     * in order to run the schedule computed by the scheduler.
     * Basically, for every consecutive pair of streams (hierarchically,
     * meaning that for example, pipeline's children know how to connect
     * to each other, and two pipeline know how to connect to each other, but
     * their children don't know how to connect to each other's children.
     */
    final Map bufferSizes = new HashMap ();

    public BigInteger getBufferSizeBetween (Object streamSrc, Object streamDst)
    {
        Object result = bufferSizes.get (streamSrc);
        if (result == null) return null;

        ASSERT (result instanceof Object []);
        Object [] dataArray = (Object []) result;

        ASSERT (dataArray [0] == streamDst);
        ASSERT (dataArray [1] instanceof BigInteger);

        return (BigInteger) dataArray [1];
    }

    public void setBufferSize (Object streamSrc, Object streamDst, BigInteger bufferSize)
    {
        ASSERT (!bufferSizes.containsKey (streamSrc));

        Object [] dataArray = new Object [2];
        dataArray [0] = streamDst;
        dataArray [1] = bufferSize;

        bufferSizes.put (streamSrc, dataArray);
    }

    List schedule = new LinkedList ();

    public List getSchedule ()
    {
        return schedule;
    }

    public void setSchedule (Object newSchedule)
    {
        if (newSchedule instanceof List)
        {
            schedule = (List) newSchedule;
        } else {
            schedule = new LinkedList ();
            schedule.add (newSchedule);
        }
    }

    public void addToSchedule (Object scheduleElement)
    {
        ASSERT ((List) scheduleElement != null || (SchedStream) scheduleElement != null);
        schedule.add (scheduleElement);
    }
}

