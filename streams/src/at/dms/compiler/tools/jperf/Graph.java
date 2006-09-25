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
 * $Id: Graph.java,v 1.3 2006-09-25 13:54:32 dimock Exp $
 */

package at.dms.compiler.tools.jperf;

import java.io.PrintWriter;
import java.util.Vector;

/**
 * This class represents the intermediate graph
 */
class Graph {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Constructs a new graph object
     */
    public Graph(long maxNodeValue) {
        this.maxNodeValue = maxNodeValue;
    }

    /**
     * Initialises the internal structures
     */
    public void init() {
        nodes = new Vector<Node>();
    }

    // --------------------------------------------------------------------
    // ACCESSORS & MUTATORS
    // --------------------------------------------------------------------

    /**
     * Adds a node to the graph.
     *
     * @param   node        the node to add
     */
    public void addNode(Node node) {
        nodes.addElement(node);
    }

    /**
     * Searches node with given label the intermediate graph.
     * @param   label       the node label
     * @return  the node with given label, null otherwise.
     */
    public Node findNode(long label) {
        if (label >= maxNodeValue) {
            System.err.println("Internal fatal error: try to find invalid vertex label " + label + ".");
            System.exit(-1);
        }

        for (int i = 0; i < nodes.size(); i++) {
            Node    node = nodes.elementAt(i);
            if (node.getLabel() == label) {
                return node;
            }
        }
        return null;
    }

    /**
     * Adds an edge (and the corresponding nodes if necessary) to the graph
     *
     * @param   label1      the label of the source node
     * @param   label2      the label of the target node
     * @param   edge        the edge value
     *
     * @return  true iff the graph is still acyclic
     */
    public boolean addEdge(long label1, long label2, long edge) {
        if (label1 >= maxNodeValue) {
            System.err.println("Internal error: invalid node label " + label1 + ".");
            System.exit(-1);
        }
        if (label2 >= maxNodeValue) {
            System.err.println("Internal error: invalid node label " + label2 + ".");
            System.exit(-1);
        }

        if (label1 == label2) {
            return false;
        }

        Node node1 = findNode(label1);
        Node node2 = findNode(label2);

        if ((node1 != null) && (node2 != null)) {
            clearVisited();

            if (node1.reaches(node2)) {
                return false;
            }
        } else {
            // need to allocate space for node1
            if (node1 == null) {
                node1 = new Node(label1);
                addNode(node1);
            }

            // need to allocate space for node2
            if (node2 == null) {
                node2 = new Node(label2);
                addNode(node2);
            }
        }

        // setup the adjacency list
        node1.addAdjacency(node2, edge);
        node2.addAdjacency(node1, edge);

        return true;
    }

    /**
     * Clears visited flags of all nodes.
     */
    public void clearVisited() {
        for (int i = 0; i < nodes.size(); i++) {
            nodes.elementAt(i).setVisited(false);
        }
    }

    /**
     * Sets g-values for all nodes.
     */
    public void assignGValues(int keywordCount) {
        for (int i = 0; i < nodes.size(); i++) {
            nodes.elementAt(i).assignGValue(0, keywordCount);
        }
    }

    // --------------------------------------------------------------------
    // CODE GENERATION
    // --------------------------------------------------------------------

    /**
     * Outputs g-values.
     * @param   out     the output stream.
     */
    public void genCode(PrintWriter out) {
        // generate constants
        out.println("  private static final int MAX_NODE_NUM = "+ nodes.size() + ";");

        int     nodeCnt = nodes.size();

        // generate gIndex


        // generate tables
        out.println("  private static final int gIndex(int n) {");
        out.println("    switch(n) {");

        // special case of zero
        for (int i = 0, outed = 0; i < nodeCnt; i++) {
            Node    node = nodes.elementAt(i);

            if (node.getGValue() == 0) {
                out.println("    case " + node.getLabel() + ":");
            }
        }
        out.println("      return 0;");
        for (int i = 0, outed = 0; i < nodeCnt; i++) {
            Node    node = nodes.elementAt(i);

            if (node.getGValue() != 0) {
                out.println("    case " + node.getLabel() + ":");
                out.println("      return " + node.getGValue() + ";");
            }
        }
        out.println("    default:");
        out.println("      return -1;");
        out.println("    }");
        out.println("  }");
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private final long      maxNodeValue;

    // The nodes in the intermediate graph
    private Vector<Node>      nodes;
}
