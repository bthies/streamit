package streamit.scheduler2.constrained;

import streamit.scheduler2.hierarchical.PhasingSchedule;

public class SteadyDownstreamRestriction extends Restriction
{
    final streamit.scheduler2.SDEPData sdep;
    
    
    SteadyDownstreamRestriction(
        P2PPortal _portal,
        streamit.scheduler2.SDEPData _sdep,
        StreamInterface _parent)
    {
        super (_portal.getDownstreamNode(), _portal);
        
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
