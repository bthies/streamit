package streamit.scheduler2;

import streamit.misc.AssertedClass;
import streamit.scheduler2.iriter.Iterator;

/**
 * <dl>
 * <dt>Purpose: Main scheduler class to be used as an interface for all 
 * schedulers
 * <dd>
 *
 * <dt>Description:
 * <dd> This class represents a scheduler which should be used by outside users
 * to obtain, optimize and find out information about a schedule. This is an
 * abstract class, and every particular scheduler should implement the missing
 * virtual classes.
 * </dl>
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

abstract public class Scheduler extends AssertedClass
{
    Schedule initSchedule = null;
    Schedule steadySchedule = null;
    Schedule optimizedInitSchedule = null;
    Schedule optimizedSteadySchedule = null;
    ScheduleBuffers scheduleBuffers = null;
    protected Iterator root;

    public Scheduler(Iterator _root)
    {
        root = _root;
    }

    /**
     * compute a schedule.
     * This function computes a schedule corresponding to a particular
     * stream structure. It must reset optimizedSchedule and scheduleBuffers
     * to null (or compute them).
     */
    abstract public void computeSchedule();
    
    void optimizeSchedule ()
    {
        if (optimizedInitSchedule == null)
        {
            ScheduleOptimizer optimizer =
                new ScheduleOptimizer(initSchedule, steadySchedule);
            optimizer.optimize();
            optimizedInitSchedule = optimizer.getOptimizedInitSched();
            optimizedSteadySchedule = optimizer.getOptimizedSteadySched();
        }
    }

    public Schedule getOptimizedInitSchedule()
    {
        ASSERT(initSchedule != null && steadySchedule != null);
        
        optimizeSchedule ();
        return optimizedInitSchedule;
    }

    public Schedule getOptimizedSteadySchedule()
    {
        ASSERT(initSchedule != null && steadySchedule != null);
        
        optimizeSchedule ();
        return optimizedSteadySchedule;
    }

    void computeBufferUse()
    {
        if (scheduleBuffers == null)
        {
            scheduleBuffers = new ScheduleBuffers(root);
            scheduleBuffers.computeBuffersFor(getOptimizedInitSchedule());
            scheduleBuffers.computeBuffersFor(getOptimizedSteadySchedule());
        }
    }

    public int getBufferSizeBetween(
        Iterator userBefore,
        Iterator userAfter)
    {
        computeBufferUse();
        return scheduleBuffers.getBufferSizeBetween(userBefore, userAfter);
    }
}
