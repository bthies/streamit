/*
 * ExprConstFloat.java: a real-valued constant
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: ExprConstFloat.java,v 1.2 2002-08-20 20:04:28 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * An real-valued constant.  This can appear in an ExprComplex to form
 * a complex real expression.
 */
public class ExprConstFloat extends Expression
{
    private double val;
    
    /** Create a new ExprConstFloat with a specified value. */
    public ExprConstFloat(FEContext context, double val)
    {
        super(context);
        this.val = val;
    }

    /** Create a new ExprConstFloat with a specified value but no context. */
    public ExprConstFloat(double val)
    {
        this(null, val);
    }
    
    /** Parse a string as a double, and create a new ExprConstFloat
     * from the result. */
    public ExprConstFloat(FEContext context, String str)
    {
        this(context, Double.parseDouble(str));
    }
    
    /** Returns the value of this. */
    public double getVal() { return val; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprConstFloat(this);
    }
}
