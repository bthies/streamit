package streamit.scheduler2.base;

import streamit.scheduler2.Schedule;
import streamit.scheduler2.iriter.Iterator;

/* $Id: StreamInterface.java,v 1.7 2002-12-02 23:54:07 karczma Exp $ */

/**
 * This interface will provide the basic functionality for
 * all future stream classes.  This will ensure that streams can
 * be used interchangably.  I have to make this an interface because
 * Java doesn't have multi-inheritance :(
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface StreamInterface
{
    /**
     * Return an iterator to this stream. The iterator is a unspecialized
     * iterator type.
     */
    public Iterator getStreamIter();
    
    /**
     * Compute the appropriate schedules for this  Stream.  This function
     * computes both the steady state and initialization schedules.
     */
    public void computeSchedule();

    /**
     * Get the steady schedule computed for this stream.
     * @return steady schedule
     */
    public Schedule getSteadySchedule();

    /**
     * Get the initialization schedule computed for this stream.
     * @return initialization schedule
     */
    public Schedule getInitSchedule();

    /**
     * return number of data peeked in a minimal steady execution
     * of this element.
     * @return number of data peeked in a steady execution.
     */
    public int getSteadyPeek();

    /**
     * return number of data popped in a minimal steady execution
     * of this element.
     * @return number of data popped in a steady execution.
     */
    public int getSteadyPop();

    /**
     * return number of data pushed in a minimal steady execution
     * of this element.
     * @return number of data pushed in a steady execution.
     */
    public int getSteadyPush();

    /**
     * return number of data peeked during intialization 
     * of this element.
     * @return number of data peeked during initialization
     */
    public int getInitPeek();

    /**
     * return number of data popped during initialization
     * of this element.
     * @return number of data popped during initialization
     */
    public int getInitPop();

    /**
     * return number of data pushed during initialization
     * of this element.
     * @return number of data pushed during initialization
     */
    public int getInitPush();


    
    public int getNumNodes ();
    
    public int getNumNodeFirings();
}
