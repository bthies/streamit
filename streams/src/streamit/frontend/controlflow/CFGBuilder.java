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
import java.util.*;

/**
 * Helper class to build a control-flow graph from linear code.
 * The {@link #buildCFG} method can be called externally to
 * produce a CFG from a function declaration.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: CFGBuilder.java,v 1.1 2004-01-16 21:32:25 dmaze Exp $
 */
public class CFGBuilder extends FENullVisitor
{
    /**
     * Build a control-flow graph from a function.  This is the main
     * entry point to this module.
     *
     * @param func  function to build a CFG for
     * @return      control-flow graph object for the function
     */
    public static CFG buildCFG(Function func)
    {
        CFGBuilder builder = new CFGBuilder();
        CFGNodePair pair = (CFGNodePair)func.getBody().accept(builder);
        return new CFG(builder.nodes, pair.start, pair.end, builder.edges);
    }

    // Visitors return this:
    private static class CFGNodePair
    {
        public CFGNode start;
        public CFGNode end;
        public CFGNodePair(CFGNode start, CFGNode end)
        {
            this.start = start;
            this.end = end;
        }
    }
    
    // Where to go for particular statements:
    private CFGNode nodeReturn, nodeBreak, nodeContinue;
    // What edges exist (map from start node to list of end node):
    private Map edges;
    // Every node:
    private List nodes;
    
    private CFGBuilder()
    {
        nodeReturn = null;
        nodeBreak = null;
        nodeContinue = null;
        edges = new HashMap();
        nodes = new ArrayList();
    }

    private void addEdge(CFGNode from, CFGNode to)
    {
        List target;
        if (edges.containsKey(from))
            target = (List)edges.get(from);
        else
        {
            target = new ArrayList();
            edges.put(from, target);
        }
        if (!target.contains(to))
            target.add(to);
    }

    private CFGNodePair visitStatement(Statement stmt)
    {
        // If the visitor works, use its result.
        CFGNodePair pair = (CFGNodePair)stmt.accept(this);
        if (pair != null)
            return pair;
        // Otherwise, create a node, add it, and return a basic pair.
        CFGNode node = new CFGNode(stmt);
        nodes.add(node);
        return new CFGNodePair(node, node);
    }
    
    public Object visitStmtBlock(StmtBlock block)
    {
        // Create entry and exit nodes for the block.
        CFGNode entry = new CFGNode(block, true);
        CFGNode exit = new CFGNode(block, true);
        nodes.add(entry);
        nodes.add(exit);

        // If we haven't declared a return point yet, this must
        // be the top-level block and so the return point is our exit.
        if (nodeReturn == null)
            nodeReturn = exit;
               
        // Also remember where we are in traversing.  Start at the
        // beginning.
        CFGNode current = entry;

        // Walk through all of the contained statements.
        for (Iterator iter = block.getStmts().iterator(); iter.hasNext(); )
        {
            Statement stmt = (Statement)iter.next();
            CFGNodePair pair = visitStatement(stmt);
            // Add an edge from our current end to the start of the pair,
            // but only if the current end is non-null ("we were going
            // somewhere").  This could lead to a node with no
            // forward path to it, but that's okay.
            if (current != null)
                addEdge(current, pair.start);
            // Make the end of the pair current.  That could be null if
            // the statement was a break, continue, or return statement
            // that doesn't have an interesting outgoing edge.
            current = pair.end;
        }

        // Add an edge from the current node to the exit, if there is
        // a current node.  (For example, current could be null if the
        // last statement in a function is a return, but that's fine.)
        if (current != null)
            addEdge(current, exit);
        
        return new CFGNodePair(entry, exit);
    }

    public Object visitStmtFor(StmtFor stmt)
    {
        // We need an exit node here, but not an explicit entry.
        CFGNodePair pairInit = visitStatement(stmt.getInit());
        CFGNode entry = pairInit.start;
        CFGNode exit = new CFGNode(stmt, true);
        nodes.add(exit);
        // Loop condition:
        CFGNode cond = new CFGNode(stmt, stmt.getCond());
        nodes.add(cond);
        // Increment:
        CFGNodePair pairIncr = visitStatement(stmt.getIncr());
        // Things we know are connected:
        // (claim that pairInit and pairIncr don't have null ends.)
        addEdge(pairInit.end, cond);
        addEdge(pairIncr.end, cond);
        addEdge(cond, exit);
        // Also, continue statements go to incr, breaks to exit.
        CFGNode lastContinue = nodeContinue;
        CFGNode lastBreak = nodeBreak;
        nodeContinue = pairIncr.start;
        nodeBreak = exit;
        // Get the child pair.
        CFGNodePair pairBody = visitStatement(stmt.getBody());
        // Restore:
        nodeContinue = lastContinue;
        nodeBreak = lastBreak;
        // Connect body.
        addEdge(cond, pairBody.start);
        if (pairBody.end != null)
            addEdge(pairBody.end, pairIncr.start);
        // And return the pair.
        return new CFGNodePair(entry, exit);
    }

    public Object visitStmtIfThen(StmtIfThen stmt)
    {
        // Entry node is the condition; exit is artificial.
        CFGNode entry = new CFGNode(stmt, stmt.getCond());
        nodes.add(entry);
        CFGNode exit = new CFGNode(stmt, true);
        nodes.add(exit);
        // Check both branches.
        if (stmt.getCons() != null)
        {
            CFGNodePair pair = visitStatement(stmt.getCons());
            addEdge(entry, pair.start);
            if (pair.end != null)
                addEdge(pair.end, exit);
        }
        else
        {
            addEdge(entry, exit);
        }

        if (stmt.getAlt() != null)
        {
            CFGNodePair pair = visitStatement(stmt.getAlt());
            addEdge(entry, pair.start);
            if (pair.end != null)
                addEdge(pair.end, exit);
        }
        else
        {
            addEdge(entry, exit);
        }
        
        return new CFGNodePair(entry, exit);
    }

    public Object visitStmtWhile(StmtWhile stmt)
    {
        // similarly.
        CFGNode entry = new CFGNode(stmt, stmt.getCond());
        nodes.add(entry);
        CFGNode exit = new CFGNode(stmt, true);
        nodes.add(exit);
        // continue statements go to cond (entry), breaks to exit.
        CFGNode lastContinue = nodeContinue;
        CFGNode lastBreak = nodeBreak;
        nodeContinue = entry;
        nodeBreak = exit;
        // Get the child pair.
        CFGNodePair pairBody = visitStatement(stmt.getBody());
        // Restore:
        nodeContinue = lastContinue;
        nodeBreak = lastBreak;
        // Connect body.
        addEdge(entry, pairBody.start);
        if (pairBody.end != null)
            addEdge(pairBody.end, entry);
        // Conditional can be false.
        addEdge(entry, exit);
        // And return the pair.
        return new CFGNodePair(entry, exit);        
    }

    public Object visitStmtDoWhile(StmtDoWhile stmt)
    {
        // A little different: artificial entry, save the condition
        // separately, since it's neither entry nor exit.
        CFGNode entry = new CFGNode(stmt, true);
        nodes.add(entry);
        CFGNode cond = new CFGNode(stmt, stmt.getCond());
        nodes.add(entry);
        CFGNode exit = new CFGNode(stmt, true);
        nodes.add(exit);
        // continue statements go to cond , breaks to exit.
        CFGNode lastContinue = nodeContinue;
        CFGNode lastBreak = nodeBreak;
        nodeContinue = cond;
        nodeBreak = exit;
        // Get the child pair.
        CFGNodePair pairBody = visitStatement(stmt.getBody());
        // Restore:
        nodeContinue = lastContinue;
        nodeBreak = lastBreak;
        // Connect body.
        addEdge(entry, pairBody.start);
        if (pairBody.end != null)
            addEdge(pairBody.end, cond);
        // Also connect up loop and exit from cond.
        addEdge(cond, pairBody.start);
        addEdge(cond, exit);
        // And return the pair.
        return new CFGNodePair(entry, exit);        
    }

    public Object visitStmtBreak(StmtBreak stmt)
    {
        // Build a node,
        CFGNode node = new CFGNode(stmt);
        nodes.add(node);
        // but explicitly connect it to the current loop break node.
        addEdge(node, nodeBreak);
        // Return an edge pair pointing to null.
        return new CFGNodePair(node, null);
    }

    public Object visitStmtContinue(StmtContinue stmt)
    {
        CFGNode node = new CFGNode(stmt);
        nodes.add(node);
        addEdge(node, nodeContinue);
        return new CFGNodePair(node, null);
    }

    public Object visitStmtReturn(StmtReturn stmt)
    {
        CFGNode node = new CFGNode(stmt);
        nodes.add(node);
        addEdge(node, nodeReturn);
        return new CFGNodePair(node, null);
    }
}

