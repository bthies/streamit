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
 * <dd> This interface describes an iterator for a Filter.
 * </dl>
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface FilterIter extends IteratorBase
{
    /**
     * Returns an Iterator that pointst to the same object as this 
     * specialized iterator.
     * @return an Iterator that points to the same object
     */
    public Iterator getUnspecializedIter();
    
    /**
     * Returns the number of init functions for this Filter.
     * @return number of init functions for this Filter
     */
    public int getNumInitStages ();
    
    /**
     * Returns the amount of data that a particular init function peeks.
     * @return peek amount of an init function
     */
    public int getInitPeekStage (int stage);

    /**
     * Returns the amount of data that a particular init function pops.
     * @return pop amount of an init function
     */
    public int getInitPopStage (int stage);

    /**
     * Returns the amount of data that a particular init function pushes.
     * @return push amount of an init function
     */
    public int getInitPushStage (int stage);
    
    /**
     * Returns a particular init function for this filter.
     * @return init function
     */
    public Object getInitFunctionStage (int stage);
    
    /**
     * Returns the number of work functions this filter has (number of
     * its phases).
     * @return number of init functions for this Filter
     */
    public int getNumWorkPhases ();

    /**
     * Returns the amount of data that a particular phase of this peeks.
     * @return peek amount for a particular work function
     */
    public int getPeekPhase (int phase);

    /**
     * Returns the amount of data that a particular phase of this pops.
     * @return pop amount for a particular work function
     */
    public int getPopPhase (int phase);

    /**
     * Returns the amount of data that a particular phase of this pushes.
     * @return push amount for a particular work function.
     */
    public int getPushPhase (int phase);
    
    /**
     * Returns a particular work function (phase of this filter).
     * @return work function
     */
    public Object getWorkFunctionPhase (int phase);
}
