package streamit.scheduler.base;

import streamit.scheduler.iriter./*persistent.*/Iterator;

/* $Id: StreamFactory.java,v 1.2 2002-06-30 04:01:06 karczma Exp $ */

/**
 * The StreamFactory interface provides a factory interface for
 * the scheduler.  Since it is the streams themselves that calculate
 * their schedules, the base.Stream class will be overwritten many
 * times, and the factory will provide a way for the user to request
 * an arbitrary arrangement of different Stream scheduling algorithms.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface StreamFactory
{
    /**
     * Given an iterator, create an appropriate stream out of it.
     * 
     * @return A stream corresponding to streamIter
     */
    public StreamInterface newFrom (Iterator streamIter);
}

