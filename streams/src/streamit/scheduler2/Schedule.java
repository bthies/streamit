package streamit.scheduler;

import streamit.misc.AssertedClass;
import java.util.Vector;

/* $Id: Schedule.java,v 1.6 2002-06-30 04:01:03 karczma Exp $ */

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
     * all the sub-schedules that are contained by this schedule
     */
    final private Vector subScheds;
    
    /**
     * Work function associated with bottom-level schedule
     */
    final private Object workFunc;
    
    /**
     * Stream iterator corresponding to the work function of this schedule.
     */
    final private streamit.scheduler.iriter.Iterator workStream;
    
    /**
     * Number of times that this schedule is supposed to be repeated
     */
    private int nRepetitions = 1;

    /**
     * Create a schedule that will be used with many sub-schedules.
     */
    public Schedule ()
    {
        subScheds = new Vector ();
        workFunc = null;
        workStream = null;
    }
    
    /**
     * Create the schedule to be a bottom schedule with a single
     * work function.
     */
    public Schedule (Object workFunction, streamit.scheduler.iriter.Iterator stream)
    {
        workFunc = workFunction;
        workStream = stream;
        subScheds = null;
    }
    
    /**
     * Checks if the Schedule is a bottom-level schedule.
     * If the Schedule contains a work function that needs
     * to be called, it is a bottom schedule.  This work function
     * is always attached to a workStream.  If it contains
     * a bunch of sub-schedules, which need to be executed
     * in order, this is not a bottom schedule.
     * @return true if this Schedule is a bottom schedule
     */
    public boolean isBottomSchedule () { return workStream != null; }
    
    /**
     * Get the number of phases in this schedule
     * @return number of phases in this schedule
     */
    public int getNumPhases ()
    {
        // make sure that I'm not a bottom schedule
        ASSERT (subScheds != null);
        return subScheds.size ();
    }

    /**
     * Get a subschedule of this schedule.   If this is the final
     * (lowest level - one that references a work function) schedule,
     * this function will ASSERT.
     * @return subschedules of this schedule
     */
    public Schedule getSubSched(int nSched)
    {
        ASSERT(subScheds != null);
        ASSERT(nSched >= 0 && nSched < subScheds.size ());
        return (Schedule) subScheds.get (nSched);
    }
    
    /**
     * Add a schedule to the list of sub-schedules of this schedule.
     * This basically appends another schedule to the subScheds,
     * thus augmenting the current schedule from the end.
     */
    public void addSubSchedule(Schedule subSchedule)
    {
        // make sure this is not a bottom schedule
        ASSERT (subScheds != null);
        
        subScheds.add(subSchedule);
    }

    /**
     * Returns the work function associated with a schedule.
     * If the schedule isn't a bottom-level schedule, this
     * function will ASSERT.
     * @return work function associated with schedule
     */
    public Object getWorkFunc()
    {
        ASSERT(isBottomSchedule ());
        return workFunc;
    }

    /**
     * Returns the stream on which the work function will be called
     * If the schedule isn't a bottom-level schedule, this
     * function will ASSERT.
     * @return schedule's stream
     */
    public streamit.scheduler.iriter.Iterator getWorkStream()
    {
        ASSERT(isBottomSchedule ());
        return workStream;
    }
    
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