/*
 * ExprConstant.java: a constant-valued expression
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: ExprConstant.java,v 1.1 2002-07-10 18:03:31 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A constant-valued expression.  This class only serves to hold
 * static methods for creating constants.
 */
abstract public class ExprConstant extends Expression
{
    /** Create a new constant-valued expression corresponding to a
     * String value.  val must be a valid real number, according to
     * java.lang.Double.valueOf(), excepting that it may end in "i" to
     * indicate an imaginary value.  This attempts to create an
     * integer if possible, and a real-valued expression if possible;
     * however, it may also create an ExprComplex with a zero (null)
     * real part.  Eexpressions like "3+4i" need to be parsed into
     * separate expressions. */
    public static Expression createConstant(String val)
    {
        // Either val ends in "i", or it doesn't.
        if (val.endsWith("i"))
        {
            val = val.substring(0, val.length()-1);
            return new ExprComplex(null, createConstant(val));
        }

        // Maybe it's integral.
        try
        {
            return new ExprConstInt(val);
        }
        catch(NumberFormatException e)
        {
            // No; create a float (and lose if this is wrong too).
            return new ExprConstFloat(val);
        }
    }
}
