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

import streamit.library.SplitJoin;
import streamit.library.NullSplitter;
import streamit.library.NullJoiner;

public class SplitJoinIter
    extends streamit.misc.DestroyedClass
    implements streamit.scheduler2.iriter.SplitJoinIter
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
    
    public streamit.scheduler2.iriter.Iterator getUnspecializedIter()
    {
        return new Iterator(splitjoin);
    }
    
    public int getNumChildren ()
    {
        return splitjoin.getNumChildren ();
    }
    
    public streamit.scheduler2.iriter.Iterator getChild (int n)
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
    
    public Object getSplitterWork(int nWork)
    {
        ASSERT(nWork >= 0 && nWork < getSplitterNumWork ());
        return  splitjoin.getSplitter();
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
    
    public Object getJoinerWork(int nWork)
    {
        ASSERT(nWork >= 0 && nWork < getJoinerNumWork ());
        return  splitjoin.getJoiner();
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
    
    public boolean equals(Object other)
    {
        if (!(other instanceof SplitJoinIter)) return false;
        SplitJoinIter otherSJ = (SplitJoinIter) other;
        return otherSJ.getObject() == this.getObject();
    }
    
    public int hashCode()
    {
        return splitjoin.hashCode();
    }
}

