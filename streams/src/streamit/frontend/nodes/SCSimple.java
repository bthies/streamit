package streamit.frontend.nodes;

import java.util.List;

/**
 * Stream creator that instantiates streams by name.  This creates a
 * stream given its name, a possibly empty list of type parameters,
 * and a parameter list.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: SCSimple.java,v 1.5 2003-07-24 16:58:37 dmaze Exp $
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

