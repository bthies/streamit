package streamit.scheduler2.constrained;

import streamit.misc.OMap;
import streamit.misc.OMapIterator;
import streamit.misc.OSet;
import streamit.misc.OSetIterator;

import streamit.scheduler2.hierarchical.PhasingSchedule;

/**
 * streamit.scheduler2.constrained.Restriction keeps track of how much
 * a given node can execute according to some constraint.
 */

abstract public class Restriction extends streamit.misc.AssertedClass
{
    final LatencyNode node;
    Restrictions restrictions;
    final P2PPortal portal;
    int maxExecution;

    public Restriction(
        LatencyNode _node,
        P2PPortal _portal)
    {
        node = _node;
        portal = _portal;
    }
    
    public void useRestrictions (Restrictions _restrictions)
    {
        ASSERT (_restrictions);
        restrictions = _restrictions;
    }

    public int getNumAllowedExecutions()
    {
        return maxExecution - restrictions.getNumExecutions(node);
    }

    public void setMaxExecutions(int _maxExecutions)
    {
        maxExecution =
            restrictions.getNumExecutions(node) + _maxExecutions;
    }

    public P2PPortal getPortal()
    {
        return portal;
    }

    public LatencyNode getNode()
    {
        return node;
    }

    /**
     * Notify the restriction that it has expired. The restriction will
     * return true if it should be removed from the Restrictions queue
     * and thus unblock execution, or false, if it should be left in the
     * queue and continue blocking execution. 
     */
    abstract public boolean notifyExpired();


    public PhasingSchedule checkMsg ()
    {
        ERROR ("ERROR!\ncheckMsg can ONLY be called on a SteadyDownstreamRestriction and SteadyUpstreamRestriction!");
        return null;
    }
}