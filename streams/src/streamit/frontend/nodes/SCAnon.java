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
 * Stream creator for anonymous streams.  It has a
 * <code>StreamSpec</code> object which completely specifies the new
 * stream being created.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: SCAnon.java,v 1.6 2003-10-09 19:50:59 dmaze Exp $
 */
public class SCAnon extends StreamCreator
{
    private StreamSpec spec;
    
    /**
     * Creates a new anonymous stream given its specification.
     *
     * @param context  file and line number this object corresponds to
     * @param spec     contents of the anonymous stream
     * @param portals  list of <code>Expression</code> giving the
     *                 portals to register the new stream with
     */
    public SCAnon(FEContext context, StreamSpec spec, List portals)
    {
        super(context, portals);
        this.spec = spec;
    }
    
    /**
     * Creates a new anonymous stream given the type of stream and
     * its init function.
     *
     * @param context  file and line number this object corresponds to
     * @param type     type of stream, as one of the constants in
     *                 <code>StreamSpec</code>
     * @param init     contents of the stream's initialization code
     * @param portals  list of <code>Expression</code> giving the
     *                 portals to register the new stream with
     */
    public SCAnon(FEContext context, int type, Statement init, List portals)
    {
        super(context, portals);
        this.spec = new StreamSpec(context, type, null, null,
                                   Collections.EMPTY_LIST, init);
    }
    
    /**
     * Returns the stream specification this creates.
     *
     * @return  specification of the child stream
     */
    public StreamSpec getSpec()
    {
        return spec;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitSCAnon(this);
    }
}
