/*
 * ExprConstChar.java: a single-character literal
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: ExprConstChar.java,v 1.1 2002-07-10 18:03:30 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A single-character literal, as appears inside single quotes in Java.
 */
public class ExprConstChar extends Expression
{
    private char val;
    
    /** Create a new ExprConstChar for a particular character. */
    public ExprConstChar(char val)
    {
        this.val = val;
    }
    
    /** Create a new ExprConstChar containing the first character of a
     * String. */
    public ExprConstChar(String str)
    {
        this(str.charAt(0));
    }
    
    /** Returns the value of this. */
    public char getVal() { return val; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprConstChar(this);
    }
}
