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
 * $Id: JClassBlock.java,v 1.3 2003-05-16 21:58:35 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.TokenReference;

/**
 * JLS 14.2: Block
 *
 * TA block is a sequence of statements and local variable declaration
 * statements within braces.
 */
public class JClassBlock extends JBlock {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    private JClassBlock() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	isStatic	is this block a static initializer ?
   * @param	body		a list of statement
   */
  public JClassBlock(TokenReference where, boolean isStatic, JStatement[] body) {
    super(where, body, null);

    this.isStatic = isStatic;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * @return	is this block a static initializer
   */
  public boolean isStaticInitializer() {
    return isStatic;
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    private /* final*/  boolean		isStatic;// removed final for cloner
}
