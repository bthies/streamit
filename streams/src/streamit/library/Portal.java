package streamit;

/**
 * A portal that messages can be sent to.  This class is not currently
 * useful in the Java library.  For the StreamIt compiler, the set of
 * messages that can be sent should be defined in an interface.  A
 * class derived from <code>Portal</code> and implementing the interface
 * whose name ends in "Portal" should be defined; that class is the portal
 * object.  Receiver objects should also implement the interface.
 *
 * @version $Id: Portal.java,v 1.1 2003-07-23 19:31:48 dmaze Exp $
 */
public class Portal
{
    public void regReceiver(Object o)
    {
        throw new UnsupportedOperationException
            ("StreamIt Java library does not support messaging");
    }
}
