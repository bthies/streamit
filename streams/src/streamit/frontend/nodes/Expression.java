package streamit.frontend.nodes;

/**
 * A generic expression tree, as created in the front-end.  Expression
 * nodes often will contain other Expressions as recursive children.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&
 * @version $Id: Expression.java,v 1.4 2003-07-30 20:10:17 dmaze Exp $
 */
abstract public class Expression extends FENode
{
    public Expression(FEContext context)
    {
        super(context);
    }

    /**
     * Determine if this expression can be assigned to.  <i>The C
     * Programming Language</i> refers to expressions that can be
     * assigned to as <i>lvalues</i>, since they can appear on the
     * left-hand side of an assignment statement.
     *
     * @return true if the expression can appear on the left-hand side
     *         of an assignment statement
     */
    public boolean isLValue()
    {
        return false;
    }
}
