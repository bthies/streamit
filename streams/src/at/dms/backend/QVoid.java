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
 * $Id: QVoid.java,v 1.1 2001-08-30 16:32:25 thies Exp $
 */

package at.dms.backend;

import at.dms.util.InconsistencyException;

/**
 * This class represents the an instruction that has no return value
 */
class QVoid extends QNode {

  public QVoid(QOrigin origin) {
    this.origin = origin;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Human readable form
   */
  public String toString() {
    return "V " +  origin;
  }

  /**
   * The type of this instruction
   */
  public int getType() {
    return at.dms.classfile.Constants.TYP_VOID;
  }

  /**
   * Returns the primitive instruction
   */
  public InstructionHandle getInstruction() {
    return origin.getInstruction();
  }

  // ----------------------------------------------------------------------
  // ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Returns the defined temporary.
   */
  public QTemporary getDef() {
    return null;
  }

  /**
   * Returns the used temporaries.
   */
  public QTemporary[] getUses() {
    return origin.getUses();
  }

  /**
   * returns the parameters of this instruction
   */
  public QOrigin[] getOrigins() {
    return origin.getOrigins();
  }

  /**
   * Sets the parameters of this instruction
   */
  public void setOrigin(QOrigin origin, int i) {
    if (this.origin instanceof QOperator) {
      this.origin.setOrigin(origin, i);
    } else if (i == 0) {
      this.origin = origin;
    } else {
      throw new InconsistencyException();
    }
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Generates instructions for this quadruple
   * @param	seq		The code sequence of instruction
   */
  public void generate(CodeSequence seq) {
    origin.generate(seq);
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private QOrigin	origin;
}
