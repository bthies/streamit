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
 * A generic expression tree, as created in the front-end.  Expression
 * nodes often will contain other Expressions as recursive children.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&
 * @version $Id: Expression.java,v 1.5 2003-10-09 19:50:59 dmaze Exp $
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
