package streamit.frontend.nodes;

/**
 * Declaration of a field in a filter or structure.  This describes
 * the declaration of a single variable with a single type and an
 * optional initialization value.  This is explicitly not a
 * <code>Statement</code>; declarations that occur inside functions
 * are local variable declarations, not field declarations.
 * Similarly, this is not a stream parameter (in StreamIt code; it may
 * be in Java code).
 *
 * @see     StmtVarDecl
 * @see     Parameter
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: FieldDecl.java,v 1.2 2003-04-16 13:34:45 dmaze Exp $
 */
public class FieldDecl extends FENode
{
    private Type type;
    private String name;
    private Expression init;
    
    /** Create a new field declaration.
     *
     * @param context  front-end context showing where the object was
     *                 created in the original source code
     * @param type     type of the field
     * @param name     name of the field
     * @param init     expression containing the initialized value for
     *                 this, or null if the field is uninitialized
     */
    public FieldDecl(FEContext context, Type type, String name,
                     Expression init)
    {
        super(context);
        this.type = type;
        this.name = name;
        this.init = init;
    }
    
    /** Get the type of variable declared by this. */
    public Type getType()
    {
        return type;
    }
    
    /** Get the name of the variable declared by this. */
    public String getName()
    {
        return name;
    }
    
    /** Get the initial value of this. */
    public Expression getInit()
    {
        return init;
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitFieldDecl(this);
    }
}
