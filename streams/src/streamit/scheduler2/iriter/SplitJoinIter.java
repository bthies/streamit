package streamit.scheduler.iriter;

import streamit.scheduler.iriter.Iterator;

/* $Id: SplitJoinIter.java,v 1.6 2002-05-27 00:11:48 karczma Exp $ */

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

public interface SplitJoinIter extends IteratorBase, SplitterIter, JoinerIter
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