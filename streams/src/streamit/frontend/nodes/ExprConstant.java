/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.frontend.nodes;

/**
 * A constant-valued expression.  This class only serves to hold
 * static methods for creating constants.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ExprConstant.java,v 1.3 2003-10-09 19:50:59 dmaze Exp $
 */
abstract public class ExprConstant extends Expression
{
    // Go Java go!  If we don't have this, the compiler complains:
    public ExprConstant(FEContext context) 
    {
        super(context);
    }
    
    /**
     * Create a new constant-valued expression corresponding to a
     * String value.  val must be a valid real number, according to
     * java.lang.Double.valueOf(), excepting that it may end in "i" to
     * indicate an imaginary value.  This attempts to create an
     * integer if possible, and a real-valued expression if possible;
     * however, it may also create an ExprComplex with a zero (null)
     * real part.  Eexpressions like "3+4i" need to be parsed into
     * separate expressions. 
     *
     * @param context  file and line number for the string
     * @param val      string containing the constant to create
     * @return         an expression corresponding to the string value
     */
    public static Expression createConstant(FEContext context, String val)
    {
        // Either val ends in "i", or it doesn't.
        if (val.endsWith("i"))
        {
            val = val.substring(0, val.length()-1);
            return new ExprComplex(context, null,
                                   createConstant(context, val));
        }

        // Maybe it's integral.
        try
        {
            return new ExprConstInt(context, val);
        }
        catch(NumberFormatException e)
        {
            // No; create a float (and lose if this is wrong too).
            return new ExprConstFloat(context, val);
        }
    }
}
