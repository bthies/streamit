/*
 * SCSimple.java: simple stream creator
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: SCSimple.java,v 1.3 2002-09-23 21:18:48 dmaze Exp $
 */

package streamit.frontend.nodes;

import java.util.List;

/**
 * SCSimple is a stream creator that creates a stream given its name,
 * a possibly empty list of type parameters, and a parameter list.
 */
public class SCSimple extends StreamCreator
{
    private String name;
    private List types;
    private List params;
    
    /** Create a stream object given its name and a parameter list. */
    public SCSimple(FEContext context, String name, List types, List params)
    {
        super(context);
        this.name = name;
        this.types = types;
        this.params = params;
    }
    

    /** Return the name of the stream created by this. */
    public String getName()
    {
        return name;
    }

    /** Return the type parameter list of the stream. */
    public List getTypes()
    {
        return types;
    }
    
    /** Return the parameter list of the stream. */
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

