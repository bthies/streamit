package streamit.scheduler2.constrained;

public class InitPeekRestriction extends Restriction
{
    public InitPeekRestriction(LatencyEdge edge, StreamInterface parent)
    {
        super(
            edge.getSrc(),
            new P2PPortal(
                true,
                edge.getSrc(),
                edge.getDst(),
                edge.getNumSrcInitPhases(),
                edge.getNumSrcInitPhases(),
                parent));
    }
    
    
    
    public boolean notifyExpired()
    {
        portal.getParent().initRestrictionsCompleted(portal);
        return true;
    }

    public void useRestrictions(Restrictions _restrictions)
    {
        super.useRestrictions(_restrictions);
        setMaxExecutions(portal.getMaxLatency());
    }
}