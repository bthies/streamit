/*
 * Parameter.java: a formal parameter or field declaration
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: Parameter.java,v 1.1 2002-09-04 15:12:56 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A Parameter is a type/name pair that may appear as a formal parameter
 * to a function or stream, or as an element in a structure declaration.
 */
public class Parameter
{
    private Type type;
    private String name;
    
    /** Creates a new Parameter with the specified type and name. */
    public Parameter(Type type, String name)
    {
        this.type = type;
        this.name = name;
    }
    
    /** Returns the type of this. */
    public Type getType()
    {
        return type;
    }
    
    /** Returns the name of this. */
    public String getName()
    {
        return name;
    }
}
