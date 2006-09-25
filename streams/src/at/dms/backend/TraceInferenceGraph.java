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
 * $Id: TraceInferenceGraph.java,v 1.4 2006-09-25 13:54:31 dimock Exp $
 */

package at.dms.backend;

import java.io.IOException;
import java.util.Vector;

/**
 * This is the entry point of the backend, this class constructs the
 * control flow graph and applies optimizations
 */
public class TraceInferenceGraph extends Trace {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Creates a new instruction handle.
     * (Probably buggy description from cut and paste -- deleted references to wrong parameters)
     */
    public TraceInferenceGraph(InferenceNode[] nodes) {
        super("infer.vcg");

        this.nodes = nodes;
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    public void run() {
        try {
            generateHeader();

            for (int i = 0; i < nodes.length; i++) {
                trace(nodes[i]);
            }

            write("}");
            close();
        } catch (IOException io) {
            System.err.println("Cannot write infer.vcg");
        }
    }

    // --------------------------------------------------------------------
    // PRIVATE METHODS
    // --------------------------------------------------------------------

    /**
     * Prints a trace of quadruples
     */
    private void trace(InferenceNode node) throws IOException {
        QTemporary[]    temps = node.getTemporaries();
        Vector<InferenceNode>      links = node.getInferences();
        String      label;


        // GENERATE LABEL
        label = "" + node.getColor();
        label += " = ";
        for (int i = 0; i < temps.length; i++) {
            label += temps[i] + (i == temps.length - 1 ? "" : ", ");
        }
        generateNode("" + node, label, node.getColor());

        // GENERATE LINKS
        for (int i = 0; i < links.size(); i++) {
            InferenceNode   dest = links.elementAt(i);

            if (node.getPosition() < dest.getPosition()) {
                write(generateEdge("" + node, "" + links.elementAt(i), i, links.size(), false));
            }
        }
    }

    private void generateNode(String pos, String label, int color) throws IOException {
        String  tcolor = color < COLORS.length ? COLORS[color] : COLORS[COLORS.length - 1];

        write("node: { title:\"" + pos + "\" label: \"" + label + "\" color: " + tcolor + "}");
    }

    private void generateHeader() throws IOException {
        write("graph: { title: \"CFG_GRAPH\"");
        write("x: 150");
        write("y: 30");
        write("width: 716");
        write("height: 960");
        write("layoutalgorithm: minbackward  ");
        write("display_edge_labels: yes");
        //write("manhatten_edges: yes");
        write("layout_nearfactor: 0");
        write("xspace: 25");
        write("node.color: lightyellow");
        write("node.textcolor: blue");
        write("edge.color: blue");
        write("edge.arrowsize: 15");
        write("edge.thickness: 1");
        write("stretch: 43");
        write("shrink: 100");
        write("classname 1 : \"CFG Edges (blue)\"");
        write("classname 2 : \"Const Lists (red)\"");
        write("classname 3 : \"Live Variable Lists (green)\"");
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private InferenceNode[]     nodes;
    private static String[]     COLORS = new String[] {
        "red",
        "green",
        "yellow",
        "magenta",
        "cyan",
        "white",
        "darkgrey",
        "darkblue",
        "darkred",
        "darkgreen",
        "darkyellow",
        "darkmagenta",
        "darkcyan",
        "gold",
        "lightgrey",
        "lightblue",
        "lightred",
        "lightgreen",
        "lightyellow",
        "lightmagenta",
        "lightcyan",
        "lilac",
        "turquoise",
        "aquamarine",
        "khaki",
        "purple",
        "yellowgreen",
        "pink",
        "orange",
        "orchid"
    };
}
