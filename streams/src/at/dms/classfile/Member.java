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
 * $Id: Member.java,v 1.1 2001-08-30 16:32:27 thies Exp $
 */

package at.dms.classfile;

/**
 * VMS 4 : Members.
 *
 * Root class for class members (fields, methods, inner classes and interfaces)
 *
 */
public abstract class Member implements Constants {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Constructs a member object.
   *
   * @param	modifiers	access permission to and properties of this member
   */
  public Member(short modifiers) {
    this.modifiers = modifiers;
  }

  /**
   * Constructs a member object.
   */
  Member() {
    this((short)0);
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  /**
   * Returns the modifiers of this member
   */
  public short getModifiers() {
    return modifiers;
  }

  /**
   * Returns the modifiers of this member
   */
  public void setModifiers(short modifiers) {
    this.modifiers = modifiers;
  }

  /**
   * Returns the name of the this member
   */
  public abstract String getName();

  /**
   * Returns the type of the this member
   */
  public abstract String getSignature();

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private short			modifiers;
}
