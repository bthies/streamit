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
 * $Id: InterfaceConstant.java,v 1.1 2001-08-30 16:32:27 thies Exp $
 */

/**
 * An InterfaceConstant is used to refer to an interface specification
 */
package at.dms.classfile;

public class InterfaceConstant extends ReferenceConstant {

  /**
   * Constructs a new method reference constant.
   *
   * @param	name	the qualified name of the referenced object
   * @param	type	the signature of the referenced object
   */
  public InterfaceConstant(String name, String type) {
    super(CST_INTERFACEMETHOD, name, type);
  }

  /**
   * Constructs a new method reference constant.
   *
   * @param	name	the qualified name of the referenced object
   * @param	type	the signature of the referenced object
   */
  public InterfaceConstant(String owner, String name, String type) {
    super(CST_INTERFACEMETHOD, owner, name, type);
  }

  /**
   * Constructs a method reference constant.
   *
   * @param	clazz		the class that defines the referenced object
   * @param	nametype	the simple name and signature of the referenced object
   */
  public InterfaceConstant(ClassConstant clazz, NameAndTypeConstant nametype) {
    super(CST_INTERFACEMETHOD, clazz, nametype);
  }
}
