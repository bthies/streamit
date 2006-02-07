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

package streamit.misc;

/**
 * This is a common interface to all the implementations of wrappable
 * growable queues.
 */
public interface WrappableGrowableQueue {

    /**
     * Returns the number of elements currently stored in the queue.
     * @return The number of elements in the queue.
     */
    int size();

    /** 
     * Changes the size of the buffer used to hold the queue. This is the only function
     * which somewhat exposes the internal workings of the object, and it is there as a
     * performance measure so that objects which know they need a queue of a certain size
     * can grow it to that size beforehand.
     * @param size The number of elements the buffer should be able to hold.
     * @throws IllegalArgumentException If one tries to set the buffer size to a value 
     *                                  smaller than the current number of elements in the queue.
     */    
    void setBufferSize(int size);
}

