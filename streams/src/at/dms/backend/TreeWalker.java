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
 * $Id: TreeWalker.java,v 1.1 2001-08-30 16:32:25 thies Exp $
 */

package at.dms.backend;

/**
 * This is the an abstract class to walk across the tree (DFS)
 */
public abstract class TreeWalker {

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
  public TreeWalker(BasicBlock[] bblocks, BasicBlock[] eblocks) {
    this.bblocks = bblocks;
    this.eblocks = eblocks;
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  /**
   * Runs the deadcode algorithm
   */
  protected void traverse() {
    // TRAVERSE NODES
    ControlFlow.setMarked(bblocks, false);
    traverseInstructions(bblocks[0]);

    for (int i = 0; i < eblocks.length; i++) {
      traverseInstructions(eblocks[i]);
    }
  }

  protected BasicBlock getBasicBlock(int which) {
    return bblocks[which];
  }

  // --------------------------------------------------------------------
  // ABSTRACT METHODS
  // --------------------------------------------------------------------

  /**
   * Processes the node
   *
   * @param	node		the node to be processed
   */
  protected abstract void processNode(QNode node);

  /**
   * Called when a branch is reached
   */
  protected void kill() {
    // Default: does nothing
  }

  /**
   * Removes the specified node
   */
  protected void removeNode(QNode node) {
    BasicBlock	block = currentBlock;
    QNode[]	quads = block.getQuadruples();

    for (int i = 0 ; i < quads.length; i++) {
      if (quads[i] == node) {
	quads[i] = new QNop(); // !!! share
	return;
      }
    }

    BasicBlock[]	successors = block.getSuccessors();
    QNode[][]		successorAccess = block.getSuccessorAccess();
    int			count = quads.length;

    for (int i = 0; i < successors.length; i++) {
      for (int j = 0; successorAccess[i] != null && j < successorAccess[i].length; j++) {
	if (successorAccess[i][j] == node) {
	  successorAccess[i][j] = new QNop(); // !!! share
	  return;
	}
      }
    }
  }

  // --------------------------------------------------------------------
  // PRIVATE METHODS
  // --------------------------------------------------------------------

  private void traverseInstructions(BasicBlock block) {
    if (block.isMarked()) {
      return;
    }
    block.setMarked(true);
    currentBlock = block;

    kill();

    boolean	changed = false;
    QNode[]	quads = block.getQuadruples();
    for (int i = 0 ; i < quads.length; i++) {
      processNode(quads[i]);
    }

    BasicBlock[]	successors = block.getSuccessors();
    QNode[][]		successorAccess = block.getSuccessorAccess();
    int			count = quads.length;

    if (successors.length > 1) {
      kill();
    }

    for (int i = 0; i < successors.length; i++) {
      for (int j = 0; successorAccess[i] != null && j < successorAccess[i].length; j++) {
	processNode(successorAccess[i][j]);
      }
      traverseInstructions(successors[i]);
    }
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private BasicBlock		currentBlock;
  private BasicBlock[]		bblocks; // List of basic blocks
  private BasicBlock[]		eblocks; // List of exception handler
}
