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
 * 
 * List is a Java equivalent of the STL const list class.
 * For usage information, check STL documentation
 * 
 */

public interface DLList_const
{
    /**
     * Make a copy of the list. This does NOT copy the elements!
     */
    public DLList copy();

    /**
     * Returns the first element of a list. 
     */
    public DLListIterator front();

    public DLListIterator begin();

    /**
     * Returns the last element of a list.
     * The last element of a list is not actually a valid element, it is the
     * element beyond the last element.
     */
    public DLListIterator end();

    public DLListIterator back();

    /**
     * Returns true iff the list has no elements in it
     */
    public boolean empty();
}
