package streamit.scheduler2.constrained;

public class FilterSteadyRestriction extends Restriction
{
    final Filter filter;
    FilterSteadyRestriction(Filter _filter, int numSteadyState)
    {
        super(
            _filter.getLatencyNode(),
            new P2PPortal(
                true,
                _filter.getLatencyNode(),
                _filter.getLatencyNode(),
                numSteadyState,
                numSteadyState,
                _filter));
        filter = _filter;
    }

    public boolean notifyExpired()
    {
        // this restriction should never be removed or unblocked!
        filter.doneSteadyState();
        return false;
    }

    public void useRestrictions(Restrictions _restrictions)
    {
        super.useRestrictions(_restrictions);
        setMaxExecutions(portal.getMaxLatency());
    }
}