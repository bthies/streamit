/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: Node.java,v 1.4 2006-09-25 13:54:32 dimock Exp $
 */

package at.dms.compiler.tools.jperf;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Node is an object class representing nodes in GGPerf's
 * intermediate graph.
 * @author Jiejun KONG
 */
class Node {
    /**
     * Creates a new node labelled as the argument in the intermediate graph.
     * @param label The label.
     */
    public Node(long l) {
        label = l;
        gValue = -1;
        adjacency = new Hashtable<Node, Long>();
        visited = false;
    }

    // --------------------------------------------------------------------
    // ACCESSORS & MUTATORS
    // --------------------------------------------------------------------

    /**
     * Return the label of the node.
     * @return The label as a long value.
     */
    public long getLabel() {
        return label;
    }

    /**
     * Return the <EM>G</EM> value of the node.
     * @return The <EM>G</EM> value as a long value.
     */
    public long getGValue() {
        return gValue;
    }

    /**
     * Add an adjacent node with its edge to the adjacency list of this node.
     * @param node The adjacent node.
     * @param edge The edge between this node and the adjacent node.
     */
    public void addAdjacency(Node node, long edge) {
        adjacency.put(node, new Long(edge));
    }

    /**
     * Return the current status of the node.
     * @return the status as a boolean value. True means having been visited.
     */
    public boolean getVisited() {
        return visited;
    }

    /**
     * During a cyclicity checking process, set the status to be the argument.
     * @param b the status. True mean having been visited.
     */
    public void setVisited(boolean b) {
        visited = b;
    }

    // --------------------------------------------------------------------
    //
    // --------------------------------------------------------------------

    /**
     * Checks wheter there is a path from this node to the target node.
     * Uses Depth First Search and marks visited nodes on the path
     *
     * @param   target      the target node
     * @return  true iff the target node is reachable
     */
    public boolean reaches(Node target) {
        if (target == this) {
            return true;
        } else {
            // depth first search the adjacency list
            setVisited(true);

            Enumeration<Node> e = adjacency.keys();
            while (e.hasMoreElements()) {
                Node    adj = e.nextElement();

                if (!adj.getVisited()) {
                    if (adj.reaches(target)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Recursively assigns g_value for all nodes that could be
     * reached from this node.
     *
     * @param   val     the value to assign to this node
     * @param   max     the maximum value (= # of keywords)
     */
    public void assignGValue(long val, int max) {
        // check for valid input
        if ((val < 0) || (val >= max)) {
            System.err.println("Invalid g-value.");
        }

        // only change if not already assigned
        if (this.gValue == -1) {
            this.gValue = val;

            Enumeration<Node> e = adjacency.keys();
            while (e.hasMoreElements()) {
                Node    adj = e.nextElement();

                val = (adjacency.get(adj).longValue() - this.gValue + max) % max;
                adj.assignGValue(val, max);
            }
        }
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    /** Label of the node. */
    private final long  label;

    /** g-value of the node. */
    private long        gValue;

    /**
     * Adjancency list of the node.
     * All adjacent nodes with correspondent edges are stored in this list.
     * For any undirected graph, if A&lt;-&gt;B, then A appears in B's
     * adjacency list while B is in A's adjacency list.
     */

    private Hashtable<Node, Long>   adjacency;

    /** Flag indicating whether the node has been visited during the
     * current cyclicity checking process.
     * Since there are indefinite number of passes to check cyclicity of
     * a graph. I can't define a flag for each pass, therefore this flag
     * needs to be cleared before every checking pass to decide reachability.
     */
    private boolean visited;
}
