package streamit.scheduler.iriter;

/* $Id: FilterIter.java,v 1.4 2002-05-25 00:29:35 karczma Exp $ */

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
    public int getPopAmount (int phase);
    public int getPushAmount (int phase);
    public Object getWorkFunctionStage (int phase);
}
