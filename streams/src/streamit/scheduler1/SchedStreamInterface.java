package streamit.scheduler;

import java.math.BigInteger;

/**
 * This interface declares all functions present in SchedStream.
 * Other interfaces for super classes of Sched objects can inherit
 * from SchedStreamInterface and then extend it to allow for easy access
 * to all the functions.
 */
public interface SchedStreamInterface
{
    public Object getStreamObject ();
    public void setProduction (int p);
    public int getProduction ();
    public void setConsumption (int c);
    public int getConsumption ();
    public void setPeekConsumption (int p);
    public int getPeekConsumption ();

    /**
     * Gets the previous stream
     */
    public SchedStream getPrevStream ();

    /**
     * Sets the previous stream
     */
    public void setPrevStream (SchedStream stream);

    public BigInteger getNumExecutions ();
    public void setNumExecutions (BigInteger n);
    public void multNumExecutions (BigInteger mult);
    public void divNumExecutions (BigInteger div);

    /**
     * Compute a steady schedule.
     * A steady schedule is defined as a schedule that does not change
     * the amount of data buffered between filters when executed.
     * It can change where the (active) data is in the buffer.
     */
    public void computeSteadySchedule ();

}
