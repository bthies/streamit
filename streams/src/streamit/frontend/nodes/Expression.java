/*
 * Expression.java: a generic front-end expression
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: Expression.java,v 1.1 2002-07-10 18:03:31 dmaze Exp $
 */

package streamit.frontend.nodes;

abstract public class Expression
{
    abstract public Object accept(FEVisitor v);
}
