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

// Author = Matthew Drake (madrake@gmail.com)

package streamit.misc;

import java.nio.BufferUnderflowException;

/** 
 * This implements a queue that uses a circular buffer which grows
 * whenever the head reaches the tail.
 *
 * Note: This comment will appear in any WrappableGrowableQueue_*.java files,
 * however the subsequent comments will only appear in this file. It should
 * be understood that all of these classes are essentially the same thing and
 * changes to one should be made to all of them.

 * Note that the addition of generics in Java 1.5 won't help the situation because
 * generics don't handle basic data types like int, double, float, etc, and we're
 * using those precisely to avoid their Object counterparts.
 */

public class WrappableGrowableQueue_int implements WrappableGrowableQueue {

    int sizeof_buffer;
    int[] buffer;
    int head;
    int tail;
    int sizeof_queue;

    /**
     * Constructs a WrappableGrowableQueue object
     */
    public WrappableGrowableQueue_int() {
        sizeof_buffer = 2;
        buffer = new int[2];
        head = 0;
        tail = 0;
        sizeof_queue = 0;
    }

    /**
     * Adds an item to the beginning of the queue.
     * @param i The item to be added.
     */
    public void enqueue(int i) {
        if (sizeof_queue == sizeof_buffer) {
            this.grow(sizeof_buffer*2);
        } 
        buffer[head] = i;
        head = (head + 1) % sizeof_buffer;
        sizeof_queue++;
    }

    /**
     * Removes an item from the end of the queue.
     * @return The object at the end of the queue
     * @throws BufferUnderflowException If the queue is already empty.
     */
    public int dequeue() {
        if (sizeof_queue == 0) {
            throw new BufferUnderflowException();
        } else {
            int return_val = buffer[tail];
            tail = (tail + 1) % sizeof_buffer;
            sizeof_queue--;
            return return_val;
        }
    }

    /**
     * Returns an element at a specified position in the
     * queue without removing it from the queue.
     * @param index The position of an element from the end of the queue: 
     *              0 is the element at the end of the queue.
     * @return The element at the index'th position in the queue. This element
     *         is not removed from the queue itself.
     * @throws BufferUnderflowException If index refers to an element not in the queue.
     */
    public int elem(int index) {
        if (index >= sizeof_queue) {
            throw new BufferUnderflowException();
        } else {
            return buffer[((tail + index) % sizeof_buffer)];
        }
    }

    /**
     * Returns the number of elements currently stored in the queue.
     * @return The number of elements in the queue.
     */
    public int size() {
        return sizeof_queue;
    }

    // Grow changes the size of the circular buffer, without affecting any of the
    // actual data in the queue or the number of elements in the queue. Note - 
    // Grow does NOT check to make sure that size is appropriate. Anything that calls
    // grow should do its own checking to make sure you aren't trying to grow an array
    // to a size that won't hold what is currently in the array. */
    private void grow(int size) {
        int new_sizeof_buffer = size;
        int[] new_buffer = new int[size];
        int new_head = 0;
        int new_tail = 0;
        int new_sizeof_queue = 0;
        while (sizeof_queue > 0) {
            new_buffer[new_head] = buffer[tail];
            new_head = (new_head + 1) % new_sizeof_buffer;
            tail = (tail + 1) % sizeof_buffer;
            sizeof_queue--;
            new_sizeof_queue++;
        }
        sizeof_buffer = new_sizeof_buffer;
        buffer = new_buffer;
        head = new_head;
        tail = new_tail;
        sizeof_queue = new_sizeof_queue;
    }

    /** 
     * Changes the size of the buffer used to hold the queue. This is the only function
     * which somewhat exposes the internal workings of the object, and it is there as a
     * performance measure so that objects which know they need a queue of a certain size
     * can grow it to that size beforehand.
     * @param size The number of elements the buffer should be able to hold.
     * @throws IllegalArgumentException If one tries to set the buffer size to a value 
     *                                  smaller than the current number of elements in the queue.
     */
    public void setBufferSize(int size) {
        if (sizeof_queue > size) {
            throw new IllegalArgumentException("sizeof_queue " + sizeof_queue + " size " + size);
        }
        this.grow(size);
    }

}










