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
        super (_portal.getUpstreamNode(), _portal);
        
        sdep = _sdep;
    }
    
    public boolean notifyExpired ()
    {
        ERROR ("not implemented");
        return false;
    }
    
    public PhasingSchedule checkMsg ()
    {
        ERROR ("not implemented");
        return null;
    }
}
