/*
 * ExprArray.java: an array element reference
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: ExprArray.java,v 1.2 2002-08-20 20:04:28 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * An array-element reference.  This is an expression like "a[n]".
 * There is a base expression (the "a") and an offset expresion
 * (the "n").
 */
public class ExprArray extends Expression
{
    private Expression base, offset;
    
    /** Creates a new ExprArray with the specified base and offset. */
    public ExprArray(FEContext context, Expression base, Expression offset)
    {
        super(context);
        this.base = base;
        this.offset = offset;
    }
    
    /** Returns the base expression of this. */
    public Expression getBase() { return base; }

    /** Returns the offset expression of this. */
    public Expression getOffset() { return offset; }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprArray(this);
    }
}
