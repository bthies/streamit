/*
 * ExprConstStr.java: a string literal
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: ExprConstStr.java,v 1.2 2002-08-20 20:04:28 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A string literal.  Are these even legal in StreamIt?
 */
public class ExprConstStr extends Expression
{
    private String val;
    
    /** Create a new ExprConstStr. */
    public ExprConstStr(FEContext context, String val)
    {
        super(context);
        this.val = val;
    }
    
    /** Returns the value of this. */
    public String getVal() { return val; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprConstStr(this);
    }
}

