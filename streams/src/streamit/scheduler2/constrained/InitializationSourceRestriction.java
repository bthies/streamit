package streamit.scheduler2.constrained;

public class InitializationSourceRestriction extends Restriction
{

    public InitializationSourceRestriction(
        StreamInterface sourceStream,
        P2PPortal _portal,
        Restrictions restrictions)
    {
        super(sourceStream, _portal, restrictions);
    }

    public boolean notifyExpired()
    {
        // okay, I now have executed the entire initialization.
        // don't do ANYTHING, but don't allow yourself to be removed either

        return false;
    }

    public void goAgain()
    {
        ERROR("not implemented yet!");
    }
}
