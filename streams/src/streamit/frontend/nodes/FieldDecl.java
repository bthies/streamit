package streamit.frontend.nodes;

/**
 * Declaration of a field in a filter or structure.  This describes
 * the declaration of a single variable with a single type, but
 * no initialization value.  This is explicitly not a <code>Statement</code>;
 * declarations that occur inside functions are local variable declarations,
 * not field declarations.  Similarly, this is not a stream parameter
 * (in StreamIt code; it may be in Java code).
 *
 * @see     StmtVarDecl
 * @see     Parameter
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: FieldDecl.java,v 1.1 2003-04-15 19:22:17 dmaze Exp $
 */
public class FieldDecl extends FENode
{
    private Type type;
    private String name;
    
    /** Create a new field declaration with a type and name. */
    public FieldDecl(FEContext context, Type type, String name)
    {
        super(context);
        this.type = type;
        this.name = name;
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
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitFieldDecl(this);
    }
}
