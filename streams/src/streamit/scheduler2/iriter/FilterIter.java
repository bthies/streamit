package streamit.scheduler.iriter;

/* $Id: FilterIter.java,v 1.5 2002-05-25 19:24:24 karczma Exp $ */

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
    public int getNumInitStages ();
    public int getInitPeekStage (int phase);
    public int getInitPopStage (int phase);
    public int getInitPushStage (int phase);
    public Object getInitFunctionStage (int phase);
    
    public int getNumWorkPhases ();
    public int getPeekPhase (int phase);
    public int getPopPhase (int phase);
    public int getPushPhase (int phase);
    public Object getWorkFunctionPhase (int phase);
}
