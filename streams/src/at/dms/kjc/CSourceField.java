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
 * $Id: CSourceField.java,v 1.2 2003-05-16 21:58:34 thies Exp $
 */

package at.dms.kjc;

/**
 * This class represents an exported member of a class (fields)
 */
public class CSourceField extends CField {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CSourceField() {} // for cloner only

  /**
   * Constructs a field export
   * @param	owner		the owner of this field
   * @param	modifiers	the modifiers on this field
   * @param	ident		the name of this field
   * @param	type		the type of this field
   * @param	deprecated	is this field deprecated
   */
  public CSourceField(CClass owner,
		      int modifiers,
		      String ident,
		      CType type,
		      boolean deprecated)
  {
    super(owner, modifiers, ident, type, deprecated);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns true iff this field is used.
   */
  public boolean isUsed() {
    return used || !isPrivate() || getIdent().indexOf("$") >= 0; // $$$
  }

  /**
   * Declares this field to be used.
   */
  public void setUsed() {
    used = true;
  }

  /**
   * declare
   */
  public void setFullyDeclared(boolean fullyDeclared) {
    this.fullyDeclared = fullyDeclared && !isFinal();
  }

  /**
   *
   */
  public boolean isFullyDeclared() {
    return fullyDeclared;
  }

  /**
   *
   */
  public int getPosition() {
    return pos;
  }

  /**
   *
   */
  public void setPosition(int pos) {
    this.pos = pos;
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private int			pos;
  private boolean		used;
  private boolean		fullyDeclared;
}
