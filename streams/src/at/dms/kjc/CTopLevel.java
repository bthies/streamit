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
 * $Id: CTopLevel.java,v 1.2 2003-05-28 05:58:42 thies Exp $
 */

package at.dms.kjc;

import java.util.Hashtable;
import java.util.Enumeration;
import at.dms.classfile.ClassInfo;
import at.dms.classfile.ClassPath;
import at.dms.compiler.Compiler;

/**
 * This class implements the conceptual directory structure for .class files
 */
public final class CTopLevel extends at.dms.util.Utils {

  // ----------------------------------------------------------------------
  // LOAD CLASS
  // ----------------------------------------------------------------------

  /**
   * Loads class definition from .class file
   */
  public static CClass loadClass(String name) {
    CClass		cl = (CClass)allLoadedClasses.get(name);

    if (cl != null) {
      // look in cache
      return cl != CClass.CLS_UNDEFINED ? cl : null;
    } else {
      ClassInfo		file = ClassPath.getClassInfo(name, true);

      cl = file == null ? CClass.CLS_UNDEFINED : new CBinaryClass(file);
      allLoadedClasses.put(name, cl);

      return cl;
    }
  }

  /**
   * @return  false if name exists for source class as source class
   *          in an other file
   * @param CClass a class to add (must be a CSourceClass)
   */
  public static boolean addSourceClass(CClass cl) {
    assert(cl instanceof CSourceClass);

    CClass	last = (CClass)allLoadedClasses.put(cl.getQualifiedName(), cl);
    return (last == null) ||
      (cl.getOwner() != null) ||
      !(last instanceof CSourceClass) ||
      last.getSourceFile() == cl.getSourceFile();
  }

  /**
   * @return a class file that contain the class named name
   * @param name the name of the class file
   */
  public static boolean hasClassFile(String name) {
    CClass		cl = (CClass)allLoadedClasses.get(name);

    if (cl == null) {
      ClassInfo		file = ClassPath.getClassInfo(name, true);

      cl = file == null ? CClass.CLS_UNDEFINED : new CBinaryClass(file);
      allLoadedClasses.put(name, cl);
    }

    assert(cl != null);
    return cl != CClass.CLS_UNDEFINED;
  }

  /**
   * Removes all source classes
   */
  public static void initSession(Compiler compiler) {
    allLoadedClasses = new Hashtable(2000);
  }

  /**
   *
   */
  public static void endSession(Compiler compiler) {
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private static Hashtable	allLoadedClasses = new Hashtable(2000);

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CTopLevel other = new at.dms.kjc.CTopLevel();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CTopLevel other) {
  super.deepCloneInto(other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
