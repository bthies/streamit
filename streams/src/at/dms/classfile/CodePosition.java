/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: CodePosition.java,v 1.1 2001-08-30 16:32:26 thies Exp $
 */

package at.dms.classfile;

/**
 * This class represent the code position of each instruction during
 * analyse of control flow.
 * Such position are constrained between a min and a max value because
 * it is not possible to know the size of jumps with forward reference
 */
public final class CodePosition {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Constructs a new position
   *
   * @param	min		the minimum position
   * @param	max		the maximum position
   */
  public CodePosition(int min, int max) {
    this.min = min;
    this.max = max;
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  /**
   * Sets the value of this object from an other one
   */
  public final void setPosition(CodePosition pos) {
    min = pos.min;
    max = pos.max;
  }

  /**
   * Add a value to min and max fields
   */
  public final void addOffset(int size) {
    min += size;
    max += size;
  }

  /**
   * Returns true if min equals max
   */
  public final boolean isFix() {
    return min == max;
  }

  /**
   * Returns a string representation.
   */
  public String toString() {
    return "[min: " + min + ", max: " + max + "]";
  }

  // --------------------------------------------------------------------
  // PUBLIC MEMBERS
  // --------------------------------------------------------------------

  public int		min;
  public int		max;
}
