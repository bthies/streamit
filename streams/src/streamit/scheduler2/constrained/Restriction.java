package streamit.scheduler2.constrained;

import streamit.misc.OMap;
import streamit.misc.OMapIterator;
import streamit.misc.OSet;
import streamit.misc.OSetIterator;

/**
 * streamit.scheduler2.constrained.Restriction keeps track of how much
 * a given stream can execute according to some constraint.
 */

abstract public class Restriction extends streamit.misc.AssertedClass
{
    final StreamInterface stream;
    final Restrictions restrictions;
    final P2PPortal portal;
    int maxExecution;

    public Restriction(
        StreamInterface _stream,
        P2PPortal _portal,
        Restrictions _restrictions)
    {
        stream = _stream;
        restrictions = _restrictions;
        portal = _portal;
    }

    public int getNumAllowedExecutions()
    {
        ASSERT(0);
        return 0;
    }

    public void setMaxExecutions(int _maxExecutions)
    {
        maxExecution =
            restrictions.getNumExecutions(stream) + _maxExecutions;
    }

    public P2PPortal getPortal()
    {
        return portal;
    }

    public StreamInterface getFilter()
    {
        return stream;
    }

    /**
     * Notify the restriction that it has expired. The restriction will
     * return true if it should be removed from the Restrictions queue
     * and thus unblock execution, or false, if it should be left in the
     * queue and continue blocking execution. 
     */
    abstract public boolean notifyExpired();
}