package streamit.scheduler2.constrained;

public class FilterInitRestriction extends Restriction
{
    FilterInitRestriction (Filter filter)
    {
        super (filter.getLatencyNode(), new P2PPortal (true, filter.getLatencyNode(), filter.getLatencyNode (), 0, 0, filter)); 
        
        setMaxExecutions(filter.getNumInitStages());
    }
    
    public boolean notifyExpired ()
    {
        return true;
    }
}