package streamit.scheduler2.constrained;

public class SteadyStateSourceRestriction extends Restriction
{

    public SteadyStateSourceRestriction(
        StreamInterface sourceStream,
        P2PPortal portal,
        Restrictions restrictions)
    {
        super(sourceStream, portal, restrictions);
    }

    public boolean notifyExpired()
    {
        // okay, I now have executed the entire initialization.
        // don't do ANYTHING, but don't allow yourself to be removed either

        return false;
    }
}
