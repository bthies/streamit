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
 * <dt>Purpose: An Iterator for StreamIt Graph
 * <dd>
 *
 * <dt>Description:
 * <dd> This interface describes an iterator for a SplitJoin.
 * </dl>
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface SplitJoinIter extends IteratorBase, SplitterNJoinerIter
{
    /**
     * Returns the number of children that this SplitJoin has.
     * If some of the children and sinks or joins, they still
     * count to this total.
     * @return number of children of this SplitJoin
     */
    public int getNumChildren();

    /**
     * Returns the n-th child of the SplitJoin.
     * If the SplitJoin doesn't have enough children (n is too large),
     * this function ASSERTS and throws a RuntimeException.
     * @return n-th child of the SplitJoin
     */
    public Iterator getChild(int nChild);
}
