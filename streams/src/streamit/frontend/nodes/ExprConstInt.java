/*
 * ExprConstInt.java: an integer-valued constant
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: ExprConstInt.java,v 1.1 2002-07-10 18:03:31 dmaze Exp $
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
    public ExprConstInt(int val)
    {
        this.val = val;
    }
    
    /** Parse a string as an integer, and create a new ExprConstInt
     * from the result. */
    public ExprConstInt(String str)
    {
        this(Integer.parseInt(str));
    }
    
    /** Returns the value of this. */
    public int getVal() { return val; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprConstInt(this);
    }
}
