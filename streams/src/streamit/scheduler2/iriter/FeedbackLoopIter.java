package streamit.scheduler.v2;

import streamit.scheduler.v2.Iterator;

/* $Id: FeedbackLoopIter.java,v 1.1 2002-04-20 19:23:01 karczma Exp $ */

/**
 * <dl>
 * <dt>Purpose: An Iterator for StreamIt Graph
 * <dd>
 *
 * <dt>Description:
 * <dd> This interface describes an iterator for a FeedbackLoop.
 * </dl>
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface FeedbackLoopIter
{
    public Iterator getChild (int n);
    
    public int getSplitterType ();
    public int getJoinerType ();
    
    public int getSplitWeight (int n);
    public int getJoinWeight (int n);
}
