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
 * $Id: QStack.java,v 1.1 2001-08-30 16:32:25 thies Exp $
 */

package at.dms.backend;

/**
 * This class represents a stack placeholder
 */
class QStack extends QTemporary {

  QStack(int type) {
    super(type);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Human readable form
   */
  public String toString() {
    return "S";
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
  }

  /**
   * Generates instructions for destination
   * @param	seq		The code sequence of instruction
   */
  public void store(CodeSequence seq, boolean isLive) {
  System.err.println("<<<<<<<<<<<<<<<<<<<<" + this);
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private int	type;
}
