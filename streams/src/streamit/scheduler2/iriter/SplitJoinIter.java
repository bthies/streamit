package streamit.scheduler.iriter;

import streamit.scheduler.iriter.Iterator;

/* $Id: SplitJoinIter.java,v 1.2 2002-05-01 23:20:05 karczma Exp $ */

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

public interface SplitJoinIter
{
	public int getNumChildren ();
    public Iterator getChild (int n);
    
    public final int NULL = 0;
    public final int WEIGHTED_ROUND_ROBIN = 1;
    public final int DUPLICATE = 2;
    public final int NUM_SJ_TYPES = 3;
    
    public int getSplitterType ();
    public int getJoinerType ();
    
    public int getSplitWeight (int n);
    public int getJoinWeight (int n);
}
