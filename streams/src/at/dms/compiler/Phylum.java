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
 * $Id: Phylum.java,v 1.2 2003-05-16 21:06:35 thies Exp $
 */

package at.dms.compiler;

import at.dms.util.Utils;

/**
 * This class represents the root class for all elements of the parsing tree
 */
public abstract class Phylum extends Utils {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

  /**
   * construct an element of the parsing tree
   * @param where the token reference of this node
   */
  public Phylum(TokenReference where) {
    this.where = where;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns the token reference of this node in the source text.
   * @return the entire token reference
   */
  public TokenReference getTokenReference() {
    return where;
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    private /* final */ TokenReference	where;		// position in the source text.  removed "final" for cloner.
}
