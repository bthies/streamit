/*
 * UnrecognizedVariableException.java: unchecked generic exception
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: UnrecognizedVariableException.java,v 1.3 2003-07-07 21:19:44 dmaze Exp $
 */

package streamit.frontend.nodes;

public class UnrecognizedVariableException extends RuntimeException
{
    public UnrecognizedVariableException(FENode node, String var)
    {
        this("Unrecognized variable: " + var + " at " + node.getContext());
    }
    
    public UnrecognizedVariableException(ExprVar var)
    {
        this(var, var.getName());
    }
    
    public UnrecognizedVariableException(String s)
    {
        super(s);
    }

    public UnrecognizedVariableException()
    {
        super();
    }
}
