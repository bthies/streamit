package streamit.scheduler;

import streamit.*;
import java.io.PrintStream;
import java.util.*;

public class SchedSplitType extends SchedObject
{
    public static final int WEIGHTED_ROUND_ROBIN = 0;
    public static final int DUPLICATE = 1;
    public static final int LAST = 2;

    final int type;
    int roundConsumption;
    List splitWeights;

    SchedSplitType (int type, List splitWeights, Object splitObject)
    {
        super (splitObject);

        ASSERT (type > -1 && type < LAST);
        ASSERT (splitWeights);
        ASSERT (splitObject);
        this.type = type;
        this.splitWeights = splitWeights;

        switch (this.type)
        {
            case WEIGHTED_ROUND_ROBIN:
                {
                    roundConsumption = 0;
                    Iterator weightIter = splitWeights.listIterator ();

                    while (weightIter.hasNext ())
                    {
                        Integer weight = (Integer) weightIter.next ();
                        ASSERT (weight);

                        roundConsumption += weight.intValue ();
                    }
                    break;
                }
            case DUPLICATE:
                {
                    roundConsumption = 1;
                    break;
                }
            default:
                ASSERT (false);
        }
    }

    public int getOutputWeight (int index)
    {
        ASSERT (index >= 0 && index < splitWeights.size ());

        Integer weight = (Integer) splitWeights.get (index);
        ASSERT (weight);
        return weight.intValue ();
    }

    public int getNumWeights ()
    {
        return splitWeights.size ();
    }

    public int getRoundConsumption ()
    {
        return roundConsumption;
    }

    public Object getSplitObject ()
    {
        return getStreamObject ();
    }

    public void printDot (PrintStream outputStream)
    {
        print(getUniqueStreamName () + " [ label=\"" + getStreamName () + "\" ]\n", outputStream);
    }

    SchedObject getFirstChild () { return this; }
    SchedObject getLastChild () { return this; }


    private String streamName = null;

    public String getStreamName ()
    {
        if (streamName == null)
        {
            streamName =  "(" + getRoundConsumption () + ") " +
                          super.getStreamName () +
                          " (";

            int i;
            for (i = 0; i < splitWeights.size (); i++)
            {
                if (i != 0) streamName = streamName + ", ";
                streamName = streamName + getOutputWeight (i);
            }

            streamName = streamName + ")";
        }

        return streamName;
    }
}
