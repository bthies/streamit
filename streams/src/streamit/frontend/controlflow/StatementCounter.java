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

import streamit.frontend.nodes.Statement;

/**
 * Data-flow analysis to count the number of times some qualifying
 * statement appears in a CFG.  Lattice values are {@link CountLattice}.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StatementCounter.java,v 1.1 2004-01-21 21:13:50 dmaze Exp $
 */
public abstract class StatementCounter extends DataFlow
{
    /**
     * Get the lattice value that is at the entry node.  At entry,
     * we haven't seen any statements yet, so the entry value is
     * the counted lattice value for zero.
     *
     * @return lattice value at the entry node of the CFG
     */
    public Lattice getInit()
    {
        return new CountLattice(0);
    }

    /**
     * Modify a lattice value by passing through a CFG node.
     * We're only particularly interested in statement nodes;
     * if the statement at a statement node qualifies (as per
     * {@link #statementQualifies}), and the input value
     * is a valued lattice value, the output is a lattice
     * value for one more statement; otherwise, the output
     * is the same as the input.
     *
     * @param node  CFG node to consider
     * @param in    lattice value at entry to the node
     * @return      lattice value at exit from the node
     */
    public Lattice flowFunction(CFGNode node, Lattice in)
    {
        // Ignore nodes that aren't statement nodes.
        if (!node.isStmt())
            return in;
        
        Statement stmt = node.getStmt();
        if (statementQualifies(stmt))
        {
            CountLattice cl = (CountLattice)in;
            if (cl.isTop() || cl.isBottom())
                return in;
            return new CountLattice(cl.getValue() + 1);
        }
        else
            return in;
    }

    /**
     * Determine if a statement should be counted.
     *
     * @return true if the statement should be counted
     */
    public abstract boolean statementQualifies(Statement stmt);
}
