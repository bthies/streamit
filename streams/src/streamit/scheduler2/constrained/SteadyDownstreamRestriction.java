package streamit.scheduler2.constrained;

import streamit.scheduler2.hierarchical.PhasingSchedule;

public class SteadyDownstreamRestriction extends Restriction
{
    final streamit.scheduler2.SDEPData sdep;

    // store which is the next execution of the upstream filter
    // that can send a msg to me that I need to worry about
    int sendUpstreamExec;

    SteadyDownstreamRestriction(
        P2PPortal _portal,
        streamit.scheduler2.SDEPData _sdep,
        StreamInterface _parent)
    {
        super(_portal.getDownstreamNode(), _portal);

        sdep = _sdep;

        if (portal.isDownstream())
        {
            ERROR("not implemented");
        }
        else
        {
            // upstream portal
            // the first time I need to worry about checking messages
            // is when I have executed maxLatency times
            maxExecution = portal.getMaxLatency();
        }
    }

    SteadyUpstreamRestriction upstreamRestr;
    public void useUpstreamRestriction(SteadyUpstreamRestriction _upstreamRestr)
    {
        upstreamRestr = _upstreamRestr;
    }

    public void notifyUpstreamRestrictionBlocked()
    {
        if (portal.isDownstream())
        {
            ERROR("not implemented");
        }
        else
        {
            // as a downstream restriction in an upstream portal
            // I do not have to worry about the upstream restriction :)
            
            // do nothing
        }
    }

    public boolean notifyExpired()
    {
        upstreamRestr.notifyDownstreamRestrictionBlocked();

        if (portal.isDownstream())
        {
            ERROR("not implemented");
        }
        else
        {
            // upstream portal

            // downstream restriction for an upstream portal
            // needs to simply figure out how much it will want
            // to execute next time, and execute that many times
            {
                // first i have to remove self, so I can start mocking
                // around with maxLatency
                restrictions.remove(this);

                // figure out how many times I can execute the downstream
                // filter, before having to worry about checking for its
                // messages again
                maxExecution += (portal.getMaxLatency() - portal.getMinLatency());

                // and add self to the set of restrictoins again
                restrictions.add(this);

            }

            // and finally, I definitely do not want to be removed
            // from the list of restrictions
            return false;
        }

        return false;
    }

    public PhasingSchedule checkMsg()
    {
        if (portal.isDownstream())
        {
            ERROR("not implemented");
        }
        else
        {
            // downstream restrictions do not check messages in
            // upstream portals! just return null
            return null;
        }

        return null;
    }
}
