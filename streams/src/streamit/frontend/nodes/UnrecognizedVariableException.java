/*
 * UnrecognizedVariableException.java: unchecked generic exception
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: UnrecognizedVariableException.java,v 1.1 2002-08-12 18:48:39 dmaze Exp $
 */

package streamit.frontend.nodes;

public class UnrecognizedVariableException extends RuntimeException
{
    public UnrecognizedVariableException(String s)
    {
        super(s);
    }

    public UnrecognizedVariableException()
    {
        super();
    }
}
