package streamit.scheduler2.constrained;

import streamit.scheduler2.hierarchical.PhasingSchedule;

public class SteadyUpstreamRestriction extends Restriction
{
    final streamit.scheduler2.SDEPData sdep;

    SteadyUpstreamRestriction(
        P2PPortal _portal,
        streamit.scheduler2.SDEPData _sdep,
        StreamInterface _parent)
    {
        super(_portal.getUpstreamNode(), _portal);

        sdep = _sdep;

        if (portal.isDownstream())
        {
            ERROR("not implemented");
        }
        else
        {
            // upstream portal
            // don't do anything - it will automatically get handled
            // in other functions :)
        }
    }

    boolean amBlocked = false;

    SteadyDownstreamRestriction downstreamRestr;
    public void useDownstreamRestriction(SteadyDownstreamRestriction _downstreamRestr)
    {
        downstreamRestr = _downstreamRestr;
    }

    public void notifyDownstreamRestrictionBlocked()
    {
        if (amBlocked)
            portal.getParent().registerNewlyBlockedSteadyRestriction(this);
    }

    public boolean notifyExpired()
    {
        downstreamRestr.notifyUpstreamRestrictionBlocked();
        if (portal.isDownstream())
        {
            ERROR("not implemented");
        }
        else
        {
            // upstream portal

            // upstream restriction on an upstream portal
            // if the downstream has notified me at least once that it's
            // processed enough data for me to check for messages,
            // tell my parent I'm ready to check messages
            if (restrictions.getNumExecutions(portal.getDownstreamNode())
                >= sdep.getDstPhase4SrcPhase(maxExecution))
                portal.getParent().registerNewlyBlockedSteadyRestriction(
                    this);

            amBlocked = true;

            return false;
        }

        ERROR("not implemented");
        return false;
    }

    public PhasingSchedule checkMsg()
    {
        if (portal.isDownstream())
        {
            ERROR("not implemented");
            return null;
        }
        else
        {
            // upstream portal

            // I've used up data provided for me by one of the downstream
            // notifications now, so decrease the # of notifications
            // available to me

            // I should increase the # of times this node can be
            // executed before the next time it has to check for messages
            {
                amBlocked = false;

                // first remove myself from the list of restrictions
                restrictions.remove(this);

                // setup the max # of times this restriction can execute
                maxExecution =
                    sdep.getSrcPhase4DstPhase(
                        sdep.getDstPhase4SrcPhase(
                            restrictions.getNumExecutions(
                                portal.getUpstreamNode()))
                            + portal.getMaxLatency())
                        - 1;

                // and don't forget to add self to the list of restrictions
                restrictions.add(this);
            }

            // update the downstream portal's allowed number of executions
            // to match this current upstream restriction :)
            {
                restrictions.remove(downstreamRestr);
                downstreamRestr.setMaxExecutions(portal.getMaxLatency());
                int downstreamMax = downstreamRestr.maxExecution;
                restrictions.add(downstreamRestr);
            }

            // upstream restriction on an upstream portal does check
            // messages when necessary
            return portal.getPortalMessageCheckPhase();
        }
    }
}
