package streamit.scheduler;

import streamit.misc.AssertedClass;

/* $Id: Schedule.java,v 1.4 2002-05-24 23:10:31 karczma Exp $ */

/**
 * <dl>
 * <dt>Purpose: Schedule for External Users
 * <dd>
 *
 * <dt>Description:
 * <dd> This class represents a schedule.  It can either hold a number 
 * of subschedules or it can hold a reference to a function that 
 * should be executed.
 * </dl>
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public class Schedule extends AssertedClass
{
    /**
     * Checks if the Schedule is a bottom-level schedule.
     * If the Schedule contains a work function that needs
     * to be called, it is a bottom schedule.  If it contains
     * a bunch of sub-schedules, which need to be executed
     * in order, this is not a bottom schedule.
     * @return true if this Schedule is a bottom schedule
     */
    public boolean isBottomSchedule () { return workFunc != null; }
    
    /**
     * all the sub-schedules that are contained by this schedule
     */
    Schedule[] subScheds;

    /**
     * Get the subschedules of this schedule.   If this is the final
     * (lowest level - one that references a work function) schedule,
     * this function will ASSERT.
     * @return subschedules of this schedule
     */
    public Schedule[] getSubScheds()
    {
        ASSERT(subScheds != null);
        return subScheds;
    }

    /**
     * Work function associated with bottom-level schedule
     */
    Object workFunc;

    /**
     * Returns the work function associated with a schedule.
     * If the schedule isn't a bottom-level schedule, this
     * function will ASSERT.
     * @return work function associated with schedule
     */
    public Object getWorkFunc()
    {
        ASSERT(workFunc != null);
        return workFunc;
    }

    /**
     * Number of times that this schedule is supposed to be repeated
     */
    private int nRepetitions;

    /**
     * Get the number of times that this schedule is supposed to be
     * repeated
     * @return number of time this schedule is supposed to be repeated
     */
    public int getNumReps()
    {
        ASSERT(nRepetitions != 0);
        return nRepetitions;
    }
}