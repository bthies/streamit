package streamit.scheduler;

import java.util.*;
import java.math.BigInteger;
import streamit.*;

public abstract class SchedStream extends AssertedClass
{
    SchedStream (Object stream)
    {
        ASSERT (stream);
        this.stream = stream;
    }

    Object stream;

    public Object getStreamObject ()
    {
        return stream;
    }

    private int consumes = -1, produces = -1, peeks = -1;

    public void setProduction (int p)
    {
        ASSERT (p >= 0);
        produces = p;
    }

    int getProduction ()
    {
        ASSERT (produces >= 0);
        return produces;
    }

    public void setConsumption (int c)
    {
        ASSERT (c >= 0);
        ASSERT (peeks == -1 ^ c <= peeks);
        consumes = c;
        if (peeks < 0) peeks = c;
    }

    int getConsumption ()
    {
        ASSERT (consumes >= 0);
        return consumes;
    }

    public void setPeekConsumption (int p)
    {
        ASSERT (p >= 0);
        ASSERT (consumes <= p);
        peeks = p;
    }

    int getPeekConsumption ()
    {
        ASSERT (peeks >= 0);
        return peeks;
    }

    private SchedStream parent;

    SchedStream GetParent ()
    {
        ASSERT (parent);
        return parent;
    }

    // This section computes a steady-state schedule for children of the stream
    private BigInteger numExecutions;

    BigInteger getNumExecutions ()
    {
        return numExecutions;
    }

    void setNumExecutions (BigInteger n)
    {
        numExecutions = n;
    }

    void multNumExecutions (BigInteger mult)
    {
        // make sure that mutliplying by something > 0
        String str = mult.toString();
        ASSERT (mult.compareTo (BigInteger.ZERO) == 1);

        numExecutions = numExecutions.multiply (mult);
    }

    void divNumExecutions (BigInteger div)
    {
        // make sure that dividing by something > 0
        ASSERT (div.compareTo (BigInteger.ZERO) == 1);
        ASSERT (numExecutions.mod (div).equals (BigInteger.ZERO));

        numExecutions = numExecutions.divide (div);
    }

    /**
     * Compute a steady schedule.
     * A steady schedule is defined as a schedule that does not change
     * the amount of data buffered between filters when executed.
     * It can change where the (active) data is in the buffer.
     */
    abstract void computeSteadySchedule ();
}
