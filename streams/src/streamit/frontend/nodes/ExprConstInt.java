/*
 * ExprConstInt.java: an integer-valued constant
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: ExprConstInt.java,v 1.3 2003-06-24 21:40:14 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * An integer-valued constant.  This can be freely promoted to an
 * ExprConstFloat.  This is always real-valued, though it can appear
 * in an ExprComplex to form a complex integer expression.
 */
public class ExprConstInt extends Expression
{
    private int val;
    
    /** Create a new ExprConstInt with a specified value. */
    public ExprConstInt(FEContext context, int val)
    {
        super(context);
        this.val = val;
    }

    /** Create a new ExprConstInt with a specified value but no context. */
    public ExprConstInt(int val)
    {
        this(null, val);
    }
    
    /** Parse a string as an integer, and create a new ExprConstInt
     * from the result. */
    public ExprConstInt(FEContext context, String str)
    {
        this(context, Integer.parseInt(str));
    }
    
    /** Returns the value of this. */
    public int getVal() { return val; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprConstInt(this);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof ExprConstInt))
            return false;
        return val == ((ExprConstInt)other).getVal();
    }
    
    public int hashCode()
    {
        return new Integer(val).hashCode();
    }
    
    public String toString()
    {
        return Integer.toString(val);
    }
}
