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
 * Exception thrown when a pass encounters an undeclared variable
 * unexpectedly.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: UnrecognizedVariableException.java,v 1.4 2003-10-09 19:51:00 dmaze Exp $
 */
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
