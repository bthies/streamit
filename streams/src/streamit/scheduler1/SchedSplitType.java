package streamit.scheduler;

import streamit.*;
import java.util.*;

public class SchedSplitType extends AssertedClass
{
    public static final int ROUND_ROBIN = 0;
    public static final int WEIGHTED_ROUND_ROBIN = 1;
    public static final int DUPLICATE = 2;
    public static final int LAST = 3;

    final int type;
    int roundConsumption;
    List splitWeights;

    public SchedSplitType (int type, List splitWeights)
    {
        ASSERT (type > -1 && type < LAST);
        ASSERT (splitWeights);
        this.type = type;
        this.splitWeights = splitWeights;

        switch (this.type)
        {
            case ROUND_ROBIN:
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

    public int getRoundConsumption ()
    {
        return roundConsumption;
    }
}
