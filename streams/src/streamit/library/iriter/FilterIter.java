package streamit.iriter;

import streamit.Filter;

public class FilterIter
    extends streamit.misc.DestroyedClass
    implements streamit.scheduler.iriter.FilterIter
{
    FilterIter(Filter _filter)
    {
        filter = _filter;
    }

    Filter filter;
    
    public Object getObject ()
    {
        return filter;
    }

    public int getNumInitStages ()
    {
        // in the library, there is only 1 stage, ever
        return 1;
    }
    
    public int getInitPeekStage (int phase)
    {
        // library has only one phase!
        ASSERT (phase == 0);
        
        // library doesn't access the tape in init
        return 0;
    }

    public int getInitPopStage (int phase)
    {
        // library has only one phase!
        ASSERT (phase == 0);
        
        // library doesn't access the tape in init
        return 0;
    }
    
    public int getInitPushStage (int phase)
    {
        // library has only one phase!
        ASSERT (phase == 0);
        
        // library doesn't access the tape in init
        return 0;
    }
    
    public Object getInitFunctionStage (int phase)
    {
        // library has only one phase!
        ASSERT (phase == 0);
        
        // just return null
        // it doesn't matter what gets returned, because init
        // functions do not access the tape in the library
        return null;
    }
    
    public int getNumWorkPhases ()
    {
        // library has only one phase!
        return 1;
    }
    
    public int getPeekPhase (int phase)
    {
        // library has only one phase!
        ASSERT (phase == 0);
        
        return filter.peekCount;
    }
    
    public int getPopPhase (int phase)
    {
        // library has only one phase!
        ASSERT (phase == 0);
        
        return filter.popCount;
    }
    
    public int getPushPhase (int phase)
    {
        // library has only one phase!
        ASSERT (phase == 0);
        
        return filter.pushCount;
    }

    public Object getWorkFunctionPhase (int phase)
    {
        // library has only one phase!
        ASSERT (phase == 0);
        
        // just return the filter
        // since the library has only one stage, the filter
        // will uniquely identify which function is meant
        return filter;
    }
}