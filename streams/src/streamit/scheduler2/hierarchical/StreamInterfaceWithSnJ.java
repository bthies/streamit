package streamit.scheduler.hierarchical;

/* $Id: StreamInterfaceWithSnJ.java,v 1.1 2002-07-16 21:41:24 karczma Exp $ */

import streamit.scheduler.Schedule;

/**
 * This interface provides the required functional interface for
 * all hierarchical scheduling objects with splits and joins.
 * This is an extension of just reguilar hierarchical scheduling
 * (as you can see from the extends statement :)
 * Basically, this takes care of getting information about
 * the split and the join in the stream
 * 
 * I have to make this an interface instead of a class because
 * Java doesn't have multiple inheritance.  Argh!
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface StreamInterfaceWithSnJ extends StreamInterface
{
    /**
     * Get the number of phases that the split of this SplitJoin has.
     * @return number of split's phases
     */
    public int getNumSplitPhases();

    /**
     * Get the appropriate phase for the split of this SplitJoin.
     * @return phase of the split
     */
    public PhasingSchedule getSplitPhase(int nPhase);

    /**
     * Get the number of phases that the join of this SplitJoin has.
     * @return number of split's join
     */
    public int getNumJoinPhases();

    /**
     * Get the appropriate phase for the join of this SplitJoin.
     * @return phase of the join
     */
    public PhasingSchedule getJoinPhase(int nPhase);
}
