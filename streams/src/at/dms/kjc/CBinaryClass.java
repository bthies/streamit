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
 * $Id: CBinaryClass.java,v 1.4 2003-05-16 21:58:34 thies Exp $
 */

package at.dms.kjc;

import java.io.*;
import java.util.Hashtable;

import at.dms.classfile.ClassInfo;
import at.dms.classfile.FieldInfo;
import at.dms.classfile.InnerClassInfo;
import at.dms.classfile.MethodInfo;

import at.dms.compiler.Compiler;

/**
 * This class represents the exported members of a binary class, i.e.
 * a class that has been compiled in a previous session.
 */
public class CBinaryClass extends CClass {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CBinaryClass() {} // for cloner only

  /**
   * Constructs a class export from file
   */
  public CBinaryClass(ClassInfo classInfo) {
    super(getOwner(classInfo.getName()),
	  classInfo.getSourceFile(),
	  classInfo.getModifiers(),
	  getIdent(classInfo.getName()),
	  classInfo.getName(),
	  classInfo.getSuperClass() == null ? null : CClassType.lookup(classInfo.getSuperClass()),
	  classInfo.isDeprecated());

    CClassType[]	interfaces = loadInterfaces(classInfo.getInterfaces());

    FieldInfo[]	fields = classInfo.getFields();
    Hashtable	fields_H = null;
    boolean	hasOuterThis = false;

    fields_H = new Hashtable(fields.length + 1, 1.0f);

    for (int i = 0; i < fields.length; i++) {
      CField field = new CBinaryField(this, fields[i]);

      if (field.getIdent().equals(JAV_OUTER_THIS)) {
	hasOuterThis = true;
      }
      fields_H.put(field.getIdent(), field);
    }

    MethodInfo[]	methods = classInfo.getMethods();
    CMethod[]		methods_V = new CMethod[methods.length];

    for (int i = 0; i < methods.length; i++) {
      methods_V[i] = new CBinaryMethod(this, methods[i]);
    }

    setInnerClasses(loadInnerClasses(classInfo.getInnerClasses()));

    setHasOuterThis(hasOuterThis);
    close(interfaces, fields_H, methods_V);
  }

  /**
   *
   */
  protected CClassType[] loadInterfaces(String[] interfaces) {
    if (interfaces != null) {
      CClassType[]	ret;
      ret = new CClassType[interfaces.length];
      for (int i = 0; i < interfaces.length; i++) {
	ret[i] = CClassType.lookup(interfaces[i]);
      }
      return ret;
    } else {
      return CClassType.EMPTY;
    }
  }

  /**
   *
   */
  protected CClassType[] loadInnerClasses(InnerClassInfo[] inners) {
    if (inners != null) {
      CClassType[] innerClasses = new CClassType[inners.length];
      for (int i = 0; i < inners.length; i++) {
	innerClasses[i] = CClassType.lookup(inners[i].getQualifiedName());
      }
      return innerClasses;
    } else {
      return CClassType.EMPTY;
    }
  }

  private static CClass getOwner(String clazz) {
    int               index = clazz.lastIndexOf("$");

    return index == -1 ? null : CClassType.lookup(clazz.substring(0, index)).getCClass();
  }
}
