package streamit.scheduler.simple;

import streamit.scheduler.simple.SimpleHierarchicalScheduler;
import streamit.scheduler.Schedule;
import java.math.BigInteger;

public class SimpleHierarchicalSchedulerPow2 extends SimpleHierarchicalScheduler
{
    class SchedulePow2 extends Schedule
    {
        public BigInteger getBufferSizeBetween (Object streamSrc, Object streamDst)
        {
            BigInteger realBufferSize = super.getBufferSizeBetween (streamSrc, streamDst);
            int shiftAmount = realBufferSize.subtract (BigInteger.valueOf (1)).bitLength ();
            return BigInteger.ONE.shiftLeft (shiftAmount);
        }
    }

    private Schedule getNewSchedule ()
    {
        return new SchedulePow2 ();
    }
}
