/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

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
 * @version $Id: StreamCreator.java,v 1.5 2004-01-30 19:33:45 dmaze Exp $
 * @see     streamit.frontend.nodes.SCAnon
 * @see     streamit.frontend.nodes.SCSimple
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
     * @return  list of <code>Expression</code> giving the portals to
     *          register the new stream with
     */
    public List getPortals()
    {
        return Collections.unmodifiableList(portals);
    }
}
