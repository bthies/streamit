package streamit.scheduler.iriter;

import streamit.scheduler.iriter.Iterator;

/* $Id: PipelineIter.java,v 1.2 2002-05-01 23:20:05 karczma Exp $ */

/**
 * <dl>
 * <dt>Purpose: An Iterator for StreamIt Graph
 * <dd>
 *
 * <dt>Description:
 * <dd> This interface describes an iterator for a Pipeline.
 * </dl>
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface PipelineIter
{
	public int getNumChildren ();
    public Iterator getChild (int n);
}
