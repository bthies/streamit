package streamit.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.math.BigInteger;
import streamit.misc.AssertedClass;

public class SchedRepSchedule extends AssertedClass
{
    final BigInteger numExecutions;
    final Object originalSchedule;

    public SchedRepSchedule (BigInteger numExecutions, Object originalSchedule)
    {
        ASSERT (originalSchedule);
        this.originalSchedule = originalSchedule;

        AssertedClass.SASSERT (numExecutions != null);
        AssertedClass.SASSERT (numExecutions.signum () > 0);
        this.numExecutions = numExecutions;
    }

    public BigInteger getTotalExecutions ()
    {
        return numExecutions;
    }

    public Object getOriginalSchedule ()
    {
        return originalSchedule;
    }
}
