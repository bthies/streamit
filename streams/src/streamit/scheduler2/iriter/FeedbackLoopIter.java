package streamit.scheduler.iriter;

import streamit.scheduler.iriter.Iterator;

/* $Id: FeedbackLoopIter.java,v 1.2 2002-05-01 23:20:04 karczma Exp $ */

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
