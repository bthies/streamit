package streamit.scheduler2.base;

import streamit.scheduler2.iriter./*persistent.*/Iterator;

/* $Id: StreamFactory.java,v 1.3 2002-12-02 23:54:07 karczma Exp $ */

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

