package streamit.scheduler2.constrained;

import streamit.scheduler2.SDEPData;

public class InitDownstreamRestriction extends Restriction
{
    final SDEPData sdep;
    final StreamInterface parent;

    public InitDownstreamRestriction(
        P2PPortal _portal,
        SDEPData _sdep,
        StreamInterface _parent,
        Restrictions restrictions)
    {
        super(_portal.getUpstreamInterface(), _portal, restrictions);

        sdep = _sdep;
        parent = _parent;

        ERROR("not implemented");
    }
    
    public boolean notifyExpired ()
    {
        return false;
    }
}
