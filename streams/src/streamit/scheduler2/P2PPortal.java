package streamit.scheduler2;

import streamit.misc.AssertedClass;

import streamit.scheduler2.iriter.Iterator;

public class P2PPortal extends AssertedClass
{
    public boolean isDownstream ()
    {
        ERROR ("Not implemented");
        return false;
    }

    public boolean isUpstream ()
    {
        return !isDownstream();
    }
    
    public Iterator getUpstreamFilter ()
    {
        ERROR ("Not implemented");
        return null;
    }

    public Iterator getDownstreamFilter ()
    {
        ERROR ("Not implemented");
        return null;
    }
    
    public int getMinLatency ()
    {
        ERROR ("Not implemented");
        return 0;
    }

    public int getMaxLatency ()
    {
        ERROR ("Not implemented");
        return 0;
    }
}