package streamit.scheduler2;

import streamit.misc.AssertedClass;
import streamit.scheduler2.iriter.Iterator;
import java.util.Map;
import java.util.HashMap;

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
    protected Schedule initSchedule = null;
    protected Schedule steadySchedule = null;
    Schedule optimizedInitSchedule = null;
    Schedule optimizedSteadySchedule = null;
    ScheduleBuffers scheduleBuffers = null;
    protected Iterator root;
    ScheduleOptimizer optimizer;

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

    void optimizeSchedule()
    {
        if (optimizer == null)
        {
            optimizer = new ScheduleOptimizer(initSchedule, steadySchedule, this);
            optimizer.optimize();
            optimizedInitSchedule = optimizer.getOptimizedInitSched();
            optimizedSteadySchedule = optimizer.getOptimizedSteadySched();
        }
    }

    public Schedule getOptimizedInitSchedule()
    {
        ASSERT(initSchedule != null && steadySchedule != null);

        optimizeSchedule();
        return optimizedInitSchedule;
    }

    public Schedule getOptimizedSteadySchedule()
    {
        ASSERT(initSchedule != null && steadySchedule != null);

        optimizeSchedule();
        return optimizedSteadySchedule;
    }

    public void computeBufferUse()
    {
        if (scheduleBuffers == null)
        {
            scheduleBuffers = new ScheduleBuffers(root);
            scheduleBuffers.computeBuffersFor(getOptimizedInitSchedule ());
            scheduleBuffers.computeBuffersFor(getOptimizedSteadySchedule ());
        }
    }

    public int getBufferSizeBetween(
        Iterator userBefore,
        Iterator userAfter)
    {
        computeBufferUse();
        return scheduleBuffers.getBufferSizeBetween(userBefore, userAfter);
    }
    
    /*
     * Schedule printing utilities
     */

    public void printSchedule(Schedule initSched, Schedule steadySched)
    {
        computeSchedule();

        Map scheds = new HashMap();

        System.out.println("init = [");
        printSched(initSched, scheds);
        System.out.println("]");

        System.out.println("steady = [");
        printSched(steadySched, scheds);
        System.out.println("]");
    }

    public void printUnoptimizedSchedule()
    {
        computeSchedule();
        printSchedule(initSchedule, steadySchedule);
    }

    public void printOptimizedSchedule()
    {
        printSchedule(
            getOptimizedInitSchedule(),
            getOptimizedSteadySchedule());
    }

    private void printSched(Schedule sched, Map scheds)
    {
        // don't print duplicates
        if (scheds.containsKey(sched))
            return;

        if (!sched.isBottomSchedule())
        {
            for (int nPhase = 0; nPhase < sched.getNumPhases(); nPhase++)
            {
                // print the children first
                printSched(sched.getSubSched(nPhase), scheds);
            }

            int symbolicIdx = scheds.size();
            scheds.put(sched, new Integer (symbolicIdx));

            // and now print self:
            System.out.print("$" + symbolicIdx + " = { ");
            for (int nPhase = 0; nPhase < sched.getNumPhases(); nPhase++)
            {
                int times = sched.getSubSchedNumExecs(nPhase);
                int idx =
                    ((Integer)scheds.get(sched.getSubSched(nPhase)))
                        .intValue();

                if (times > 1)
                    System.out.print("{" + times + " $" + idx + "} ");
                else
                    System.out.print("$" + idx + " ");

            }
            System.out.println("}");
        }
        else
        {
            // this is an actual leaf - create a vector with just
            // a single entry - the schedule
            int symbolicIdx = scheds.size();
            scheds.put(sched, new Integer (symbolicIdx));
            System.out.println(
                "$"
                    + symbolicIdx
                    + " = "
                    + sched.getStream().getObject()
                    + "."
                    + sched.getWorkFunc());
        }
    }
}
