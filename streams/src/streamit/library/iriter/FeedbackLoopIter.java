package streamit.iriter;

import streamit.FeedbackLoop;
import streamit.Splitter;
import streamit.Joiner;

public class FeedbackLoopIter
    extends streamit.misc.DestroyedClass
    implements streamit.scheduler.iriter.FeedbackLoopIter
{
    FeedbackLoopIter (FeedbackLoop _feedback)
    {
        feedback = _feedback;
    }

    FeedbackLoop feedback;
    
    public Object getObject ()
    {
        return feedback;
    }
    
    public streamit.scheduler.iriter.Iterator getBody ()
    {
        return new Iterator (feedback.getBody ());
    }

    public streamit.scheduler.iriter.Iterator getLoop ()
    {
        return new Iterator (feedback.getLoop ());
    }

    public int getSplitterType ()
    {
        return feedback.getSplitter ().getType ();
    }
    
    public int getJoinerType ()
    {
        return feedback.getJoiner ().getType ();
    }
    
    public int getNumWorkSplitter ()
    {
        switch (getSplitterType ())
        {
            case SplitJoinIter.SJ_WEIGHTED_ROUND_ROBIN:
            case SplitJoinIter.SJ_DUPLICATE:
                return 1;
            default: 
                return 0;
        }
    }
    
    public int getNumWorkJoiner ()
    {
        switch (getJoinerType ())
        {
            case SplitJoinIter.SJ_WEIGHTED_ROUND_ROBIN:
                return 1;
            default: 
                return 0;
        }
    }
    
    public int[] getSplitWeights (int n)
    {
        return feedback.getSplitter ().getWeights ();
    }
    
    public int[] getJoinWeights (int n)
    {
        return feedback.getJoiner ().getWeights ();
    }

}

