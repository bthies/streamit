package streamit.scheduler.iriter;

/* $Id: IteratorBase.java,v 1.1 2002-05-25 00:29:36 karczma Exp $ */

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
}
