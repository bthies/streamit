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
 * $Id: HandleCreator.java,v 1.1 2001-08-30 16:32:27 thies Exp $
 */

package at.dms.classfile;

import at.dms.util.ArrayLocator;

/**
 * This class replaces all references to instructions by their associated handle.
 * At the same time, it adds line numbers to the handles and checks if they
 * are references by an instruction, handler or local variable info.
 */
class HandleCreator implements AccessorTransformer {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Creates a new HandleCreator object
   * @param	insns			the array of instructions
   * @param	handles			the array of instruction handles
   */
  public HandleCreator(Instruction[] insns, InstructionHandle[] handles) {
    this.locator = new ArrayLocator(insns);
    this.handles = handles;
  }

  /**
   * Transforms the specified accessor.
   * @param	accessor		the accessor to transform
   * @return	the transformed accessor
   */
  public InstructionAccessor transform(InstructionAccessor accessor,
				       AccessorContainer container)
    throws BadAccessorException
  {
    int		index = locator.getIndex(accessor);

    if (index == -1) {
      throw new BadAccessorException("not in array: " + "container: " + container + ", accessor: " + accessor);
    }

    handles[index].attachTo(container);

    return handles[index];
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private final ArrayLocator		locator;
  private final InstructionHandle[]	handles;
}
