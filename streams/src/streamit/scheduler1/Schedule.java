package streamit.scheduler;

import java.util.*;
import java.math.BigInteger;

import streamit.AssertedClass;

public class Schedule extends AssertedClass
{
    /**
     * This map holds the sizes of buffers necessary for all structures
     * in the schedule (filters, streams, splitjoins and feedback loops).
     * The key is the structure and the value is an array size 2 of BigInteger.
     * The first element in the array is the size of input buffer, the
     * second element in the array is the size of the output buffer.
     */
    final Map bufferSizes = new HashMap ();

    public BigInteger getInputBufferSize (SchedStream stream)
    {
        Object result = bufferSizes.get (stream);
        if (result == null) return null;

        BigInteger [] sizesArray = (BigInteger []) result;
        ASSERT (sizesArray);

        return sizesArray [0];
    }

    public BigInteger getOutputBufferSize (SchedStream stream)
    {
        Object result = bufferSizes.get (stream);
        if (result == null) return null;

        BigInteger [] sizesArray = (BigInteger []) result;
        ASSERT (sizesArray);

        return sizesArray [1];
    }

    public void setBufferSizes (SchedStream stream, BigInteger inputSize, BigInteger outputSize)
    {
        ASSERT (!bufferSizes.containsKey (stream));

        BigInteger [] sizesArray = new BigInteger [2];
        sizesArray [0] = inputSize;
        sizesArray [1] = outputSize;

        bufferSizes.put (stream, sizesArray);
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
