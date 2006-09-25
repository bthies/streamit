package streamit.scheduler1;

import java.util.*;
import java.math.BigInteger;

import streamit.misc.AssertedClass;

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
    final Map<Object, Object> bufferSizes = new HashMap<Object, Object> ();

    public BigInteger getBufferSizeBetween (Object streamSrc, Object streamDst)
    {
        Object result = bufferSizes.get (streamSrc);
        if (result == null) return null;

        Object [] dataArray = null;
        if (result instanceof Object[])
            {
                dataArray = (Object []) result;
            } else {
                ASSERT (result instanceof List);

                List dataList = (List) result;
                ListIterator iter = dataList.listIterator ();
                while (iter.hasNext ())
                    {
                        dataArray = (Object[]) iter.next ();
                        ASSERT (dataArray);

                        if (dataArray [0] == streamDst) break;
                        dataArray = null;
                    }
                ASSERT (dataArray);
            }

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

    /**
     * Set buffer sizes for a join.  Since a join has many outputs, you can
     * set a buffer size for it many times, each time specifying a different
     * destination.  Setting a buffer size for the same join and destination
     * is not legal - an assertion will throw its hands up.
     */
    public void setJoinBufferSize (Object join, Object streamDst, BigInteger bufferSize)
    {
        List<Object[]> dataList = null;
        if (bufferSizes.containsKey (join))
            {
                Object result = bufferSizes.get (join);
                ASSERT (result != null && result instanceof List);
                dataList = (List<Object[]>) result;

                // make sure that this is the first destionation so far
                {
                    ListIterator<Object[]> iter = dataList.listIterator ();
                    while (iter.hasNext ())
                        {
                            Object [] dataArray = iter.next ();
                            ASSERT (dataArray);

                            ASSERT (dataArray [0] != streamDst);
                        }
                }
            } else {
                dataList = new LinkedList<Object[]> ();
                bufferSizes.put (join, dataList);
            }

        Object [] dataArray = new Object [2];
        dataArray [0] = streamDst;
        dataArray [1] = bufferSize;

        dataList.add (dataArray);
    }

    /**
     * Set a buffer size between a stream and a join.  Luckily, since every
     * source is only added once, I can simply call setBufferSize :)
     */
    public void setSplitBufferSize (Object streamSrc, Object join, BigInteger bufferSize)
    {
        setBufferSize (streamSrc, join, bufferSize);
    }

    List<Object> steadySchedule = null;
    List<Object> initSchedule = null;

    public List<Object> getSteadySchedule ()
    {
        ASSERT (steadySchedule);
        checkSchedule ((List<Object>)steadySchedule);
        return steadySchedule;
    }

    public List<Object> getInitSchedule ()
    {
        ASSERT (initSchedule);
        checkSchedule ((List<Object>)initSchedule);
        return initSchedule;
    }

    void checkSchedule (List<Object> schedule)
    {
        ASSERT (schedule);

        ListIterator<Object> iter = schedule.listIterator ();
        while (iter.hasNext ())
            {
                Object c = iter.next ();
                ASSERT (c);

                if (c instanceof List) checkSchedule ((List<Object>)c);
                ASSERT (! (c instanceof SchedStream));
            }
    }

    public void setSchedules (Object steadySchedule, Object initSchedule)
    {
        if (steadySchedule instanceof List)
            {
                this.steadySchedule = (List<Object>) steadySchedule;
            } else {
                this.steadySchedule = new LinkedList<Object> ();
                this.steadySchedule.add (steadySchedule);
            }

        if (initSchedule instanceof List)
            {
                this.initSchedule = (List<Object>) initSchedule;
            } else {
                this.initSchedule = new LinkedList<Object> ();
                this.initSchedule.add (initSchedule);
            }
    }
}

