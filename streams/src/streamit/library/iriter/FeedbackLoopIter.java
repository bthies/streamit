package streamit.iriter;

import streamit.FeedbackLoop;
import streamit.NullSplitter;
import streamit.NullJoiner;

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

    public int getFanOut () { return 2; }

    public int getSplitterNumWork ()
    {
        if (feedback.getSplitter() instanceof NullSplitter)
        {
            return 0;
        } else {
            return 1;
        }
    }
    
    public int getJoinerNumWork ()
    {
        if (feedback.getJoiner() instanceof NullJoiner)
        {
            return 0;
        } else {
            return 1;
        }
    }
    
    public int[] getSplitPushWeights (int nWork)
    {
        return feedback.getSplitter ().getWeights ();
    }
    
    public int getFanIn () { return 2; }

    public int[] getJoinPopWeights (int nWork)
    {
        return feedback.getJoiner ().getWeights ();
    }
    
    public int getSplitPop (int nWork)
    {
        return feedback.getSplitter ().getConsumption ();
    }
    
    public int getJoinPush (int nWork)
    {
        return feedback.getJoiner ().getProduction ();
    }
}

