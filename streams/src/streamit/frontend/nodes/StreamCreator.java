/*
 * StreamCreator.java: base class for stream creation expressions
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StreamCreator.java,v 1.1 2002-09-04 15:12:57 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * StreamCreator is a base class for stream creation expressions.  These
 * appear as the body of 'add', 'body', and 'loop' statements.
 */
public abstract class StreamCreator extends FENode
{
    public StreamCreator(FEContext context)
    {
        super(context);
    }
}
