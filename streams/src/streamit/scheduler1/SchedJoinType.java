package streamit.scheduler;

import streamit.*;
import java.util.List;
import java.util.Iterator;

public class SchedJoinType extends AssertedClass
{
    public static final int ROUND_ROBIN = 0;
    public static final int WEIGHTED_ROUND_ROBIN = 1;
    public static final int LAST = 2;

    final int type;
    int roundProduction;
    List joinWeights;
    Object joinObject;

    SchedJoinType (int type, List joinWeights, Object joinObject)
    {
        ASSERT (type > -1 && type < LAST);
        ASSERT (joinWeights);
        ASSERT (joinObject);
        this.type = type;
        this.joinWeights = joinWeights;
        this.joinObject = joinObject;

        switch (this.type)
        {
            case ROUND_ROBIN:
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
            default:
                ASSERT (false);
        }
    }

    int getInputWeight (int index)
    {
        ASSERT (index >= 0 && index < joinWeights.size ());

        Integer weight = (Integer) joinWeights.get (index);
        ASSERT (weight);
        return weight.intValue ();
    }

    public int getRoundProduction ()
    {
        return roundProduction;
    }

    public Object getJoinObject ()
    {
        ASSERT (joinObject);
        return joinObject;
    }
}
