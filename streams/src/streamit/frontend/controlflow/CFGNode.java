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

package streamit.frontend.controlflow;

import streamit.frontend.nodes.*;

/**
 * A single-statement or single-expression node in a control flow graph.
 * This class holds at most one expression or one statement; if it
 * holds an expression, that expression must be associated with a
 * statement.  A node can be designated <i>empty</i>, in which case
 * it is a special node used to designate entry or exit from the
 * composite statement identified in the statement.  Otherwise, if its
 * expression is non-null, the node is a conditional node, and the
 * expression must be boolean-valued.  Otherwise, the node is a statement
 * node.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: CFGNode.java,v 1.1 2004-01-16 21:32:25 dmaze Exp $
 */
public class CFGNode
{
    private boolean empty;
    private Statement stmt;
    private Expression expr;
    
    // can't both be empty and have an expression.
    private CFGNode(Statement stmt, Expression expr, boolean empty)
    {
        this.stmt = stmt;
        this.expr = expr;
        this.empty = empty;
    }

    /**
     * Create a statement node.
     *
     * @param stmt  Statement associated with the node
     */
    public CFGNode(Statement stmt)
    {
        this(stmt, null /* expr */, false /* empty */);
    }

    /**
     * Create either a placeholder or a statement node.
     *
     * @param stmt  Statement associated with the node
     * @param empty true if this is a placeholder node, false if this is
     *              a statement node
     */
    public CFGNode(Statement stmt, boolean empty)
    {
        this(stmt, null /* expr */, empty);
    }
    
    /**
     * Create an expression node.
     *
     * @param stmt  Statement associated with the node
     * @param expr  Expression associated with the node
     */
    public CFGNode(Statement stmt, Expression expr)
    {
        this(stmt, expr, false /* empty */);
    }
    
    /**
     * Determine if this node is a placeholder node.
     * If so, the statement associated with the node identifies a
     * composite statement, such as a loop, that this is a header
     * or footer for.
     *
     * @return true if this is an empty (placeholder) node
     */
    public boolean isEmpty()
    {
        return empty;
    }
    
    /**
     * Get the expression associated with an expression node.
     * Returns <code>null</code> if this is not an expression
     * node; in that case, it is either a statement or a
     * placeholder node.
     *
     * @return  expression associated with the node, or null
     */
    public Expression getExpr()
    {
        return expr;
    }
    
    /**
     * Get the statement associated with a node.  Every node has
     * a statement associated with it: for a placeholder node,
     * the statement identifies the containing loop, and for an
     * expression node, the statement identifies the statement
     * containing the expression.
     *
     * @return  statement associated with the node
     */
    public Statement getStmt()
    {
        return stmt;
    }
}
