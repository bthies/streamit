package streamit.frontend.nodes;

/**
 * A boolean-valued constant.  This can be freely promoted to any
 * other type; if converted to a real numeric type, "true" has value
 * 1, "false" has value 0.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ExprConstBoolean.java,v 1.1 2003-05-13 21:45:30 dmaze Exp $
 */
public class ExprConstBoolean extends Expression
{
    private boolean val;
    
    /**
     * Create a new ExprConstBoolean with a specified value.
     *
     * @param context  Context indicating file and line number this
     *                 constant was created in
     * @param val      Value of the constant
     */
    public ExprConstBoolean(FEContext context, boolean val)
    {
        super(context);
        this.val = val;
    }

    /**
     * Create a new ExprConstBoolean with a specified value but no
     * context.
     *
     * @param val  Value of the constant
     */
    public ExprConstBoolean(boolean val)
    {
        this(null, val);
    }
    
    /** Returns the value of this. */
    public boolean getVal() { return val; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprConstBoolean(this);
    }
}
