package streamit.scheduler.v2;

import streamit.AssertedClass;

/* $Id: Schedule.java,v 1.1 2002-04-20 19:23:03 karczma Exp $ */

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
    Schedule [] subScheds;
   
	/**
	 * get the subschedules of this schedule.   If this is the final
	 * (lowest level - one that references a work function) schedule,
	 * this function will ASSERT.
	 * @return subschedules of this schedule
	 */
	final Schedule [] getSubScheds ()
	{
	    ASSERT (subScheds != null);
	    return subScheds;
	}    
	
	/**
	 * work function associated with bottom-level schedule
	 */
	Object workFunc;
	
	/**
	 * returns the work function associated with a schedule.
	 * If the schedule isn't a bottom-level schedule, this
	 * function will ASSERT.
	 * @return work function associated with schedule
	 */
	Object getWorkFunc ()
	{
	    ASSERT (workFunc != null);
	    return workFunc;
	}
    
    
    /**
     * number of times that this schedule is supposed to be repeated
     */
    private int nRepetitions;
    
    /**
     * get the number of times that this schedule is supposed to be
     * repeated
     * @return number of time this schedule is supposed to be repeated
     */
    public int getNumReps ()
    {
        ASSERT (nRepetitions != 0);
        return nRepetitions;
    }
}
