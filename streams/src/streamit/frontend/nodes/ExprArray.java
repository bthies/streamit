package streamit.frontend.nodes;

/**
 * An array-element reference.  This is an expression like
 * <code>a[n]</code>.  There is a base expression (the "a") and an
 * offset expresion (the "n").
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ExprArray.java,v 1.3 2003-07-30 20:10:17 dmaze Exp $
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

    /**
     * Determine if this expression can be assigned to.  Array
     * elements can always be assigned to.
     *
     * @return always true
     */
    public boolean isLValue()
    {
        return true;
    }

    public String toString()
    {
        return base + "[" + offset + "]";
    }
    
    public int hashCode()
    {
        return base.hashCode() ^ offset.hashCode();
    }
    
    public boolean equals(Object o)
    {
        if (!(o instanceof ExprArray))
            return false;
        ExprArray ao = (ExprArray)o;
        return (ao.base.equals(base) && ao.offset.equals(offset));
    }
}
