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

package streamit.scheduler2.iriter;

/**
 * <dl>
 * <dt>Purpose: Base iterator for StreamIt stream graph to provide
 * some common functionality for all iterators.
 * <dd>
 *
 * <dt>Description:
 * <dd> This class contains some simple functionality that will be
 * common to all iterators used by the scheduler and implemented by
 * the user of the scheduler.
 * </dl>
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface IteratorBase
{
    /**
     * Returns the object the iterator points to.
     */
    public Object getObject ();

    /**
     * Returns equality between this and another iterator.
     * This function must check that the two iterators are equivalent
     * (point to the same stream), and not just compare the references.
     * 
     * This function is already defined in Object.  It is included in this
     * interface description to indicate that the iterators need to
     * override the default function
     * 
     * @return true if this iterator is the same as the other iterator
     */
    public boolean equals(Object other);
    
    /**
     * Returns a hash code of an iterator.  If two iterators are equivalent,
     * they must return the same value.
     *
     * This function is already defined in Object.  It is included in this
     * interface description to indicate that the iterators need to
     * override the default function
     * 
     * @return hash code for this iterator
     */
    public int hashCode();
}
