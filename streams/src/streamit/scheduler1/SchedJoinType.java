package streamit.scheduler;

import streamit.*;
import java.io.PrintStream;
import java.util.List;
import java.util.Iterator;

public class SchedJoinType extends SchedObject
{
    public static final int WEIGHTED_ROUND_ROBIN = 0;
    public static final int NULL = 1;
    public static final int LAST = 2;

    final int type;
    int roundProduction;
    List joinWeights;

    SchedJoinType (int type, List joinWeights, Object joinObject)
    {
        super (joinObject);

        ASSERT (type > -1 && type < LAST);
        ASSERT (joinWeights);
        ASSERT (joinObject);
        this.type = type;
        this.joinWeights = joinWeights;

        switch (this.type)
        {
            case WEIGHTED_ROUND_ROBIN:
                {
                    roundProduction = 0;
                    Iterator weightIter = joinWeights.listIterator ();

                    while (weightIter.hasNext ())
                    {
                        Integer weight = (Integer) weightIter.next ();
                        ASSERT (weight);

                        roundProduction += weight.intValue ();
                    }
                    break;
                }
            case NULL:
            	{
            	    roundProduction = 0;
            	    break;
            	}
            default:
                ASSERT (false);
        }
    }

    public int getInputWeight (int index)
    {
        ASSERT (index >= 0 && index < joinWeights.size ());

        Integer weight = (Integer) joinWeights.get (index);
        ASSERT (weight);
        return weight.intValue ();
    }

    public int getNumWeights ()
    {
        return joinWeights.size ();
    }

    public int getRoundProduction ()
    {
        return roundProduction;
    }

    public Object getJoinObject ()
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
            streamName = "(";

            int i;
            for (i = 0; i < joinWeights.size (); i++)
            {
                if (i != 0) streamName = streamName + ", ";
                streamName = streamName + getInputWeight (i);
            }

            streamName =  streamName + ") " +
                          super.getStreamName () +
                          " (" + getRoundProduction () + ") ";

        }

        return streamName;
    }
}
