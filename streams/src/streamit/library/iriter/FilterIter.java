package streamit.iriter;

import streamit.Filter;

public class FilterIter
    extends streamit.misc.DestroyedClass
    implements streamit.scheduler2.iriter.FilterIter
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

    public streamit.scheduler2.iriter.Iterator getUnspecializedIter()
    {
        return new Iterator(filter);
    }
    
    public int getNumInitStages ()
    {
        // in the library, there are no init stages - initialization
        // is already done!
        return 0;
    }
    
    public int getInitPeekStage (int stage)
    {
        // library doesn't have an init stage!
        ASSERT (false);
        return 0;
    }

    public int getInitPopStage (int stage)
    {
        // library doesn't have an init stage!
        ASSERT (false);
        return 0;
    }
    
    public int getInitPushStage (int stage)
    {
        // library doesn't have an init stage!
        ASSERT (false);
        return 0;
    }
    
    public Object getInitFunctionStage (int stage)
    {
        // library doesn't have an init stage!
        ASSERT (false);
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
    
    public boolean equals(Object other)
    {
        if (!(other instanceof FilterIter)) return false;
        FilterIter otherFilter = (FilterIter) other;
        return otherFilter.getObject() == this.getObject();
    }
    
    public int hashCode()
    {
        return filter.hashCode();
    }
}