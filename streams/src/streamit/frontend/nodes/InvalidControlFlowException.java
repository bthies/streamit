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
 * Exception thrown when invalid control flow exists.  The primary use
 * of this exception is to indicate dynamic control flow in regions
 * where only static control flow is allowed, such as in a phased work
 * function:
 *
 * <pre>
 * float-&gt;float filter Foo {
 *   int count;
 *   work {
 *     getCount();
 *     for (int i = 0; i &lt; count; i++) doIt();
 *   }
 *   phase getCount pop 1 { ... }
 *   phase doIt pop 1 push 1 { ... }
 * }
 * </pre>
 *
 * This may also be thrown by passes unable to handle any control flow
 * at all.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: InvalidControlFlowException.java,v 1.2 2003-10-09 19:50:59 dmaze Exp $
 */
public class InvalidControlFlowException extends RuntimeException
{
    public InvalidControlFlowException() 
    {
        super();
    }

    public InvalidControlFlowException(String s)
    {
        super(s);
    }
}

