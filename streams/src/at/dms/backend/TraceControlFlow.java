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
 * $Id: TraceControlFlow.java,v 1.1 2001-08-30 16:32:25 thies Exp $
 */

package at.dms.backend;

import java.io.IOException;

/**
 * This is the entry point of the backend, this class constructs the
 * control flow graph and applies optimizations
 */
public class TraceControlFlow extends Trace {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Creates a new instruction handle.
   *
   * @param	insn		the instruction
   * @param	prev		the handle of the next instruction
   *				in textual order
   */
  public TraceControlFlow(BasicBlock[] bblocks, BasicBlock[] eblocks) {
    super("trace.vcg");
    this.bblocks = bblocks;
    this.eblocks = eblocks;
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  public void run() {
    try {
      generateHeader();

      ControlFlow.setMarked(bblocks, false);
      trace(bblocks[0]);

      for (int i = 0; i < eblocks.length; i++) {
	trace(eblocks[i]);
      }

      write("}");
      close();
    } catch (IOException io) {
      System.err.println("Cannot write trace.vcg");
    }
  }

  // --------------------------------------------------------------------
  // PRIVATE METHODS
  // --------------------------------------------------------------------

  /**
   * Prints a trace of quadruples
   */
  private void trace(BasicBlock block) throws IOException {
    if (block.isMarked()) {
      return;
    }
    block.setMarked(true);

    System.err.println("/------------------------\\ [" + block.getPosition() + "]");

    QNode[]	quads = block.getQuadruples();
    String	label = "";

    for (int i = 0; i < quads.length; i++) {
      label += quads[i] + (i == quads.length - 1 ? "" : "\n");
    }
    System.err.print(label);

    generateBlock(block.getPosition(), label);

    System.err.println("\\------------------------/ [" + block.getPosition() + "]");

    BasicBlock[]	successors = block.getSuccessors();
    QNode[][]		successorAccess = block.getSuccessorAccess();

    for (int i = 0; i < successors.length; i++) {
      System.err.println("----> " + successors[i].getPosition());
      generateLink(block.getPosition(), successors[i].getPosition(), i, successors.length);
      for (int j = 0; successorAccess[i] != null && j < successorAccess[i].length; j++) {
	System.err.println("  " + successorAccess[i][j]);
      }
    }

    if (successors.length == 0) {
      write("edge: { sourcename:\"" + block.getPosition() + "\" targetname:\"end\" class: 1}");
    }

    System.err.println();

    for (int i = 0; i < successors.length; i++) {
      trace(successors[i]);
    }
  }

  private void generateBlock(int pos, String label) throws IOException {
    write(generateNode("" + pos, label));
  }

  private void generateLink(int pos, int toPos, int nb, int count) throws IOException {
    write(generateEdge("" + pos, "" + toPos, nb, count, true));
  }

  private void generateHeader() throws IOException {
    write("graph: { title: \"CFG_GRAPH\"");
    write("x: 150");
    write("y: 30");
    write("width: 716");
    write("height: 960");
    write("layoutalgorithm: minbackward  ");
    write("display_edge_labels: yes");
    write("manhatten_edges: yes");
    write("layout_nearfactor: 0");
    write("xspace: 25");
    write("node.color: lightyellow");
    write("node.textcolor: blue");
    write("edge.color: blue");
    write("edge.arrowsize: 15");
    write("edge.thickness: 4");
    write("stretch: 43");
    write("shrink: 100");
    write("classname 1 : \"CFG Edges (blue)\"");
    write("classname 2 : \"Const Lists (red)\"");
    write("classname 3 : \"Live Variable Lists (green)\"");
    write("node: { title:\"begin\" label: \"Start\" shape: ellipse color: aquamarine }");
    write("node: { title:\"end\" label: \"End\" shape: ellipse color: aquamarine }");
    write("edge: { sourcename:\"begin\" targetname:\"" + 0 + "\" class: 1}");
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private BasicBlock[]			bblocks; // List of basic blocks
  private BasicBlock[]			eblocks; // List of exception handler
}
