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
 * $Id: QLiteral.java,v 1.1 2001-08-30 16:32:25 thies Exp $
 */

package at.dms.backend;

import at.dms.classfile.PushLiteralInstruction;

/**
 * This class represents a literal
 */
class QLiteral extends QOperand {

  QLiteral(InstructionHandle insn) {
    this.insn = insn;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * The size (in instruction) of the subtree
   */
  public int getType() {
    return insn.getInstruction().getReturnType();
  }

  /**
   * Human readable form
   */
  public String toString() {
    return "" + ((PushLiteralInstruction)insn.getInstruction()).getLiteral();
  }

  /**
   * Duplicate this node
   */
  public QOrigin duplicate() {
    return new QLiteral(new InstructionHandle(insn.getInstruction(), null));
  }

  // ----------------------------------------------------------------------
  // ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Returns the used temporaries.
   */
  public QTemporary[] getUses() {
    return QTemporary.EMPTY;
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Generates instructions for this quadruple
   * @param	seq		The code sequence of instruction
   */
  public void generate(CodeSequence seq) {
    seq.plantInstruction(insn);
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private InstructionHandle insn;
}
