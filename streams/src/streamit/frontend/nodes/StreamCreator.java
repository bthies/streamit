package streamit.frontend.nodes;

import java.util.Collections;
import java.util.List;

/**
 * Base class for stream creation expressions.  This gives some sort
 * of description of a child stream of a composite stream object, and
 * appears as the body of <code>add</code>, <code>body</code>, and
 * <code>loop</code> statements.  It can also specify a list of
 * portals that the newly created child is registered with.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StreamCreator.java,v 1.2 2003-07-07 15:48:18 dmaze Exp $
 * @see     streamit.frontend.nodes.SCAnon, streamit.frontend.nodes.SCSimple
 */
public abstract class StreamCreator extends FENode
{
    private List portals;
    
    /**
     * Create a new stream creator with a list of portals.
     *
     * @param context  file and line number this object corresponds to
     * @param portals  list of <code>Expression</code> giving the portals
     *                 to register the new stream with
     */
    public StreamCreator(FEContext context, List portals)
    {
        super(context);
        if (portals == null)
            portals = Collections.EMPTY_LIST;
        this.portals = portals;
    }

    /**
     * Get the list of portals the new stream is registered with.
     *
     * @returns  list of <code>Expression</code> giving the portals to
     *           register the new stream with
     */
    public List getPortals()
    {
        return Collections.unmodifiableList(portals);
    }
}
