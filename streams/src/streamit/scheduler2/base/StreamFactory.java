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

package streamit.scheduler2.base;

import streamit.scheduler2.iriter./*persistent.*/Iterator;

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
    public StreamInterface newFrom (Iterator streamIter, Iterator parent);
}

