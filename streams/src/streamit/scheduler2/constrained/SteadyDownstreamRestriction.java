package streamit.scheduler2.constrained;

public class SteadyDownstreamRestriction extends Restriction
{
    final streamit.scheduler2.SDEPData sdep;
    
    
    SteadyDownstreamRestriction(
        P2PPortal _portal,
        streamit.scheduler2.SDEPData _sdep,
        StreamInterface _parent,
        Restrictions _restrictions)
    {
        super (_parent, _portal, _restrictions);
        
        sdep = _sdep;
    }
    
    public boolean notifyExpired ()
    {
        ERROR ("not implemented");
        return false;
    }
}
