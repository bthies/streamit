package streamit.scheduler.v2;

import streamit.scheduler.v2.Iterator;

/* $Id: PipelineIter.java,v 1.1 2002-04-20 19:23:03 karczma Exp $ */

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
