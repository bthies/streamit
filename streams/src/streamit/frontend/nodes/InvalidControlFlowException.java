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
 * @version $Id: InvalidControlFlowException.java,v 1.1 2003-02-10 19:48:21 dmaze Exp $
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

