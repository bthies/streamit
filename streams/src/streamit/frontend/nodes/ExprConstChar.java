/*
 * ExprConstChar.java: a single-character literal
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: ExprConstChar.java,v 1.2 2002-08-20 20:04:28 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A single-character literal, as appears inside single quotes in Java.
 */
public class ExprConstChar extends Expression
{
    private char val;
    
    /** Create a new ExprConstChar for a particular character. */
    public ExprConstChar(FEContext context, char val)
    {
        super(context);
        this.val = val;
    }
    
    /** Create a new ExprConstChar containing the first character of a
     * String. */
    public ExprConstChar(FEContext context, String str)
    {
        this(context, str.charAt(0));
    }
    
    /** Returns the value of this. */
    public char getVal() { return val; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprConstChar(this);
    }
}
