/*
 * Statement.java: a generic front-end statement
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: Statement.java,v 1.1 2002-08-22 23:01:47 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A generic statement, as created in the front-end.
 */
abstract public class Statement extends FENode
{
    public Statement(FEContext context)
    {
        super(context);
    }
}
