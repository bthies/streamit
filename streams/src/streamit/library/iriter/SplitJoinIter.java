package streamit.iriter;

import streamit.SplitJoin;
import streamit.Splitter;
import streamit.Joiner;

public class SplitJoinIter
    extends streamit.misc.DestroyedClass
    implements streamit.scheduler.iriter.SplitJoinIter
{
    SplitJoinIter (SplitJoin _splitjoin)
    {
        splitjoin = _splitjoin;
    }

    SplitJoin splitjoin;
    
    public int getNumChildren ()
    {
        return splitjoin.getNumChildren ();
    }
    
    public streamit.scheduler.iriter.Iterator getChild (int n)
    {
        return new Iterator (splitjoin.getChildN (n));
    }

    public int getSplitterType ()
    {
        return splitjoin.getSplitter ().getType ();
    }
    
    public int getJoinerType ()
    {
        return splitjoin.getJoiner ().getType ();
    }
    
    public int getNumWorkSplitter ()
    {
        switch (getSplitterType ())
        {
            case SJ_WEIGHTED_ROUND_ROBIN:
            case SJ_DUPLICATE:
                return 1;
            default: 
                return 0;
        }
    }
    
    public int getNumWorkJoiner ()
    {
        switch (getJoinerType ())
        {
            case SJ_WEIGHTED_ROUND_ROBIN:
                return 1;
            default: 
                return 0;
        }
    }
    
    public int[] getSplitWeights (int n)
    {
        return splitjoin.getSplitter ().getWeights ();
    }
    
    public int[] getJoinWeights (int n)
    {
        return splitjoin.getJoiner ().getWeights ();
    }

}

