package streamit.frontend.nodes;

/**
 * A reference to a named field of a StreamIt structure.  This is
 * the expression "foo.bar".  It contains a "left" expression
 * and the name of the field being referenced.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ExprField.java,v 1.3 2003-06-30 20:23:12 dmaze Exp $
 */
public class ExprField extends Expression
{
    private Expression left;
    private String name;
    
    /** Creates a new field-reference expression, referencing the
     * named field of the specified expression. */
    public ExprField(FEContext context, Expression left, String name)
    {
        super(context);
        this.left = left;
        this.name = name;
    }
    
    /** Returns the expression we're taking a field from. */
    public Expression getLeft() { return left; }  

    /** Returns the name of the field. */
    public String getName() { return name; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprField(this);
    }

    public String toString()
    {
        return left + "." + name;
    }
    
    public boolean equals(Object other)
    {
        if (!(other instanceof ExprField))
            return false;
        ExprField that = (ExprField)other;
        if (!(this.left.equals(that.left)))
            return false;
        if (!(this.name.equals(that.name)))
            return false;
        return true;
    }
    
    public int hashCode()
    {
        return left.hashCode() ^ name.hashCode();
    }
}
