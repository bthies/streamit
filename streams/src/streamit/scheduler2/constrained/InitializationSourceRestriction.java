package streamit.scheduler2.constrained;

public class InitializationSourceRestriction extends Restriction
{
    int runNTimes;
    
    public InitializationSourceRestriction(P2PPortal _portal)
    {
        super(_portal.getUpstreamNode(), _portal);
        
        runNTimes = 0;
    }

    public boolean notifyExpired()
    {
        // okay, I now have executed the entire initialization.
        // don't do ANYTHING, but don't allow yourself to be removed either

        return false;
    }

    public void goAgain()
    {
        // allow the source stream to run twice as many times as it did
        // last time
        runNTimes = 2 * runNTimes + 1;
        
        // remove and add self to the restrictions - that'll unblock me :)
        restrictions.remove(this);
        restrictions.add(this);
    }

    public void useRestrictions(Restrictions _restrictions)
    {
        super.useRestrictions(_restrictions);
        setMaxExecutions(runNTimes);
    }
}
