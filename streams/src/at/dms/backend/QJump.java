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
 * $Id: QJump.java,v 1.1 2001-08-30 16:32:25 thies Exp $
 */

package at.dms.backend;

/**
 * This class represents the origin of a quadruple
 */
class QJump extends QVoid  {

  public QJump(QOrigin origin) {
    super(origin);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * isJump
   */
  public boolean isJump() {
    return true;
  }

  /**
   * Returns this node a a jump
   */
  public QJump getJump() {
    return this;
  }

  /**
   * Returns the target of this jump
   */
  public BasicBlock getTarget() {
    return (BasicBlock)getInstruction().getJump().getTarget();
  }
}
