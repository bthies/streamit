/*
 * ExprField.java: a field-reference expression
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: ExprField.java,v 1.2 2002-08-20 20:04:28 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A reference to a named field of a StreamIt structure.  This is
 * the expression "foo.bar".  It contains a "left" expression
 * and the name of the field being referenced.
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
}
