/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.scheduler2;

import streamit.misc.AssertedClass;
import streamit.misc.Pair;
import java.util.Vector;

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
     * Stream iterator corresponding to this schedule.
     */
    final private streamit.scheduler2.iriter.Iterator workStream;

    /**
     * Create a schedule that will be used with many sub-schedules.
     */
    public Schedule(streamit.scheduler2.iriter.Iterator stream)
    {
        subScheds = new Vector();
        workFunc = null;
        workStream = stream;
    }

    /**
     * Create the schedule to be a bottom schedule with a single
     * work function.
     */
    public Schedule(
        Object workFunction,
        streamit.scheduler2.iriter.Iterator stream)
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
    public boolean isBottomSchedule()
    {
        return subScheds == null;
    }

    /**
     * Get the number of phases in this schedule
     * @return number of phases in this schedule
     */
    public int getNumPhases()
    {
        // make sure that I'm not a bottom schedule
        ASSERT(subScheds != null);
        return subScheds.size();
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
        ASSERT(nSched >= 0 && nSched < subScheds.size());
        return (Schedule) ((Pair)subScheds.get(nSched)).getFirst();
    }

    /**
     * Get the number of times a subschedule is to be executed.
     * @return number of times a subschedule is to be executed
     */
    public int getSubSchedNumExecs(int nSched)
    {
        ASSERT(subScheds != null);
        ASSERT(nSched >= 0 && nSched < subScheds.size());
        return ((Integer) ((Pair)subScheds.get(nSched)).getSecond()).intValue();
    }

    /**
     * Add a schedule to the list of sub-schedules of this schedule.
     * This basically appends another schedule to the subScheds,
     * thus augmenting the current schedule from the end.
     */
    public void addSubSchedule(Schedule subSchedule)
    {
        // make sure this is not a bottom schedule
        ASSERT(subScheds != null);

        subScheds.add(new Pair(subSchedule, new Integer(1)));
    }

    /**
     * Add a schedule to be executed a certain # of times to the 
     * list of sub-schedules of this schedule.
     * This basically appends another schedule to the subScheds,
     * thus augmenting the current schedule from the end.
     */
    public void addSubSchedule(Schedule subSchedule, int numExecs)
    {
        // make sure this is not a bottom schedule
        ASSERT(subScheds != null);

        subScheds.add(new Pair(subSchedule, new Integer(numExecs)));
    }

    /**
     * Returns the work function associated with a schedule.
     * If the schedule isn't a bottom-level schedule, this
     * function will ASSERT.
     * @return work function associated with schedule
     */
    public Object getWorkFunc()
    {
        ASSERT(isBottomSchedule());
        return workFunc;
    }

    /**
     * Returns the stream to which this schedule phase corresponds.
     * @return schedule's stream
     */
    public streamit.scheduler2.iriter.Iterator getStream()
    {
        return workStream;
    }
}
