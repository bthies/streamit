package streamit.iriter;

import streamit.SplitJoin;
import streamit.NullSplitter;
import streamit.NullJoiner;

public class SplitJoinIter
    extends streamit.misc.DestroyedClass
    implements streamit.scheduler.iriter.SplitJoinIter
{
    SplitJoinIter (SplitJoin _splitjoin)
    {
        splitjoin = _splitjoin;
    }

    SplitJoin splitjoin;
    
    public Object getObject ()
    {
        return splitjoin;
    }
    
    public int getNumChildren ()
    {
        return splitjoin.getNumChildren ();
    }
    
    public streamit.scheduler.iriter.Iterator getChild (int n)
    {
        return new Iterator (splitjoin.getChildN (n));
    }

    public int getFanOut () { return getNumChildren (); }

    public int getSplitterNumWork ()
    {
        if (splitjoin.getSplitter() instanceof NullSplitter)
        {
            return 0;
        } else {
            return 1;
        }
    }
    
    public int getJoinerNumWork ()
    {
        if (splitjoin.getJoiner() instanceof NullJoiner)
        {
            return 0;
        } else {
            return 1;
        }
    }
    
    public int[] getSplitPushWeights (int nWork)
    {
        return splitjoin.getSplitter ().getWeights ();
    }
    
    public int getFanIn () { return getNumChildren (); }
    
    public int[] getJoinPopWeights (int nWork)
    {
        return splitjoin.getJoiner ().getWeights ();
    }
    
    public int getSplitPop (int nWork)
    {
        return splitjoin.getSplitter ().getConsumption ();
    }
    
    public int getJoinPush (int nWork)
    {
        return splitjoin.getJoiner ().getProduction ();
    }
}

