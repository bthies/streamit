/*
 * SCSimple.java: simple stream creator
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: SCSimple.java,v 1.1 2002-09-04 15:12:56 dmaze Exp $
 */

package streamit.frontend.nodes;

import java.util.List;

/**
 * SCSimple is a stream creator that creates a non-parameterized stream
 * given its name and a parameter list.
 */
public class SCSimple extends StreamCreator
{
    private String name;
    private List params;
    
    /** Create a stream object given its name and a parameter list. */
    public SCSimple(FEContext context, String name, List params)
    {
        super(context);
        this.name = name;
        this.params = params;
    }
    

    /** Return the name of the stream created by this. */
    public String getName()
    {
        return name;
    }
    
    /** Return the parameter list of the stream. */
    public List getParams()
    {
        return params;
    }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        //return v.visitSCSimple(this);
        return null;
    }
}

