package streamit.scheduler2.iriter;

import streamit.scheduler2.iriter.Iterator;

/* $Id: PipelineIter.java,v 1.6 2002-12-02 23:54:11 karczma Exp $ */

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

public interface PipelineIter extends IteratorBase
{
    /**
     * Returns an Iterator that pointst to the same object as this 
     * specialized iterator.
     * @return an Iterator that points to the same object
     */
    public Iterator getUnspecializedIter();
    
    /**
     * Returns the number of children this pipeline has.
     * @return number of children
     */
    public int getNumChildren ();
    
    /**
     * Returns an Iterator to a particular child of this pipeline.
     * @return a child
     */
    public Iterator getChild (int n);
}
