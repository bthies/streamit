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

import java.util.List;

/**
 * Stream creator that instantiates streams by name.  This creates a
 * stream given its name, a possibly empty list of type parameters,
 * and a parameter list.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: SCSimple.java,v 1.6 2003-10-09 19:50:59 dmaze Exp $
 */
public class SCSimple extends StreamCreator
{
    private String name;
    private List types;
    private List params;
    
    /**
     * Create a stream object given its name and a parameter list.
     *
     * @param context  file and line number this object corresponds to
     * @param name     name of the stream class to instantiate
     * @param types    list of <code>Type</code> giving the parameterized
     *                 type list for templated stream types
     * @param params   list of <code>Expression</code> giving the
     *                 parameter list for the stream
     * @param portals  list of <code>Expression</code> giving the
     *                 portals to register the new stream with
     */
    public SCSimple(FEContext context, String name, List types, List params,
                    List portals)
    {
        super(context, portals);
        this.name = name;
        this.types = types;
        this.params = params;
    }
    

    /** Return the name of the stream created by this. */
    public String getName()
    {
        return name;
    }

    /**
     * Return the type parameter list of the stream.  This parameter list
     * is used by templated stream types, such as the built-in
     * <code>Identity</code> stream.
     *
     * @return  list of <code>Type</code>
     */
    public List getTypes()
    {
        return types;
    }
    
    /**
     * Return the parameter list of the stream.
     *
     * @return  list of <code>Expression</code>
     */
    public List getParams()
    {
        return params;
    }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitSCSimple(this);
    }
}

