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

import java.util.*;

/**
 * Abstract base class for data-flow analyses.  This provides the
 * basic algorithm, but still leaves some parts to be filled in
 * by derived classes.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: DataFlow.java,v 1.1 2004-01-21 21:04:30 dmaze Exp $
 */
public abstract class DataFlow
{
    /**
     * Actually perform the data-flow analysis.
     *
     * @param cfg  control-flow graph to perform analysis on
     * @return     map of CFGNode to Lattice element at entry to the node
     */
    public Map run(CFG cfg)
    {
        // This is the worklist algorithm from Muchnik,
        // _Advanced Compiler Design and Implementation_, 1st ed.,
        // Section 8.4, p. 232.
        CFGNode entry;
        if (isForward())
            entry = cfg.getEntry();
        else
            entry = cfg.getExit();
        Lattice init = getInit();
        Lattice top = init.getTop();
        Lattice bottom = init.getBottom();
        Map dfin = new HashMap();

        // Set up initial worklist:
        final Set worklist = new HashSet(cfg.getNodes());
        worklist.remove(entry);
        
        // Set up initial dfin:
        dfin.put(entry, init);
        for (Iterator iter = worklist.iterator(); iter.hasNext(); )
            dfin.put(iter.next(), top);

        while (!worklist.isEmpty())
        {
            // Get an arbitrary element from the list.
            CFGNode b = (CFGNode)worklist.iterator().next();
            worklist.remove(b);
            Lattice totaleffect = top;
            
            List preds;
            if (isForward())
                preds = cfg.getPredecessors(b);
            else
                preds = cfg.getSuccessors(b);
            for (Iterator iter = preds.iterator(); iter.hasNext(); )
            {
                CFGNode p = (CFGNode)iter.next();
                Lattice in = (Lattice)dfin.get(p);
                Lattice effect = flowFunction(p, in);
                totaleffect = totaleffect.meet(effect);
            }

            if (!totaleffect.equals(dfin.get(b)))
            {
                // entry to this node has changed; save new value
                // and put successors in worklist
                dfin.put(b, totaleffect);
                if (isForward())
                    worklist.addAll(cfg.getSuccessors(b));
                else
                    worklist.addAll(cfg.getPredecessors(b));
            }
        }

        return dfin;
    }

    /**
     * Get the lattice value that is at the entry node.  What value
     * this is depends on the particular analysis being performed.
     *
     * @return lattice value at the entry node of the CFG
     */
    public abstract Lattice getInit();

    /**
     * Modify a lattice value by passing through a CFG node.
     *
     * @param node  CFG node to consider
     * @param in    lattice value at entry to the node
     * @return      lattice value at exit from the node
     */
    public abstract Lattice flowFunction(CFGNode node, Lattice in);

    /**
     * Determine if this is a forward or backward data-flow analysis.
     *
     * @return true if this is a forward analysis
     */
    public boolean isForward()
    {
        return true;
    }
}
