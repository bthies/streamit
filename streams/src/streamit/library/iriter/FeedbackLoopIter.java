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

package streamit.library.iriter;

import streamit.library.FeedbackLoop;
import streamit.library.NullSplitter;
import streamit.library.NullJoiner;

public class FeedbackLoopIter
    extends streamit.misc.DestroyedClass
    implements streamit.scheduler2.iriter.FeedbackLoopIter
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
    
    public streamit.scheduler2.iriter.Iterator getUnspecializedIter()
    {
        return new Iterator(feedback);
    }
    
    public int getDelaySize()
    {
        return feedback.getDelay();
    }
    
    public streamit.scheduler2.iriter.Iterator getBodyChild ()
    {
        return new Iterator (feedback.getBody ());
    }

    public streamit.scheduler2.iriter.Iterator getLoopChild ()
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
    
    public Object getSplitterWork(int nWork)
    {
        ASSERT(nWork >= 0 && nWork < getSplitterNumWork ());
        return  feedback.getSplitter();
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
    
    public Object getJoinerWork(int nWork)
    {
        ASSERT(nWork >= 0 && nWork < getJoinerNumWork ());
        return  feedback.getJoiner();
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
    
    public boolean equals(Object other)
    {
        if (!(other instanceof FeedbackLoopIter)) return false;
        FeedbackLoopIter otherLoop = (FeedbackLoopIter) other;
        return otherLoop.getObject() == this.getObject();
    }
    
    public int hashCode()
    {
        return feedback.hashCode();
    }
}

