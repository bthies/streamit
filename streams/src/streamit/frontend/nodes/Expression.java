/*
 * Expression.java: a generic front-end expression
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: Expression.java,v 1.3 2002-08-20 20:04:28 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A generic expression tree, as created in the front-end.  Expression
 * nodes often will contain other Expressions as recursive children.
 */
abstract public class Expression extends FENode
{
    public Expression(FEContext context)
    {
        super(context);
    }
}
