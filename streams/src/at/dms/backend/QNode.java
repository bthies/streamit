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
 * $Id: QNode.java,v 1.1 2001-08-30 16:32:25 thies Exp $
 */

package at.dms.backend;

import at.dms.util.InconsistencyException;

/**
 * This class represent an abstract node
 */
abstract class QNode  {

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * isJump
   */
  public boolean isJump() {
    return false;
  }

  /**
   * Returns this node a a jump
   */
  public QJump getJump() {
    throw new InconsistencyException();
  }

  /**
   * isJump
   */
  public boolean isSwitch() {
    return false;
  }

  /**
   * Returns this node a a jump
   */
  public QSwitch getSwitch() {
    throw new InconsistencyException();
  }

  /**
   * isStore
   */
  public boolean isStore() {
    return false;
  }

  /**
   * Returns this node a a jump
   */
  public QQuadruple getStore() {
    throw new InconsistencyException();
  }

  /**
   * Returns this node a a jump
   */
  public boolean hasSideEffect() {
    return true;
  }

  /**
   * Returns the primitive instruction
   */
  public InstructionHandle getInstruction() {
    throw new InconsistencyException();
  }

  /**
   * returns the parameters of this instruction
   */
  public abstract QOrigin[] getOrigins();

  /**
   * Sets the parameters of this instruction
   */
  public abstract void setOrigin(QOrigin origin, int i);

  // ----------------------------------------------------------------------
  // ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Returns the defined temporary.
   */
  public abstract QTemporary getDef();

  /**
   * Returns the used temporaries.
   */
  public abstract QTemporary[] getUses();

  /**
   * Returns the livein temporary.
   */
  public QTemporary[] getLivein() {
    return livein;
  }

  /**
   * Sets the livein temporary.
   */
  public void setLivein(QTemporary[] livein) {
    this.livein = livein;
  }

  /**
   * Returns the liveout temporary.
   */
  public QTemporary[] getLiveout() {
    return liveout;
  }

  /**
   * Sets the liveout temporary.
   */
  public void setLiveout(QTemporary[] liveout) {
    this.liveout = liveout;
  }

  /**
   * Is a temporary live at a certin point
   */
  public boolean isLive(QTemporary temp) {
    for (int i = 0; i < liveout.length; i++) {
      if (temp == liveout[i]) {
	return true;
      }
    }

    return false;
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Generates instructions for this quadruple
   * @param	seq		The code sequence of instruction
   */
  public abstract void generate(CodeSequence seq);

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  private	QTemporary[]	livein = QTemporary.EMPTY;
  private	QTemporary[]	liveout = QTemporary.EMPTY;
}
