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
 * $Id: CParseCompilationUnitContext.java,v 1.5 2003-11-13 10:46:10 thies Exp $
 */

package at.dms.kjc;

import java.util.Vector;
import java.util.Stack;

import at.dms.compiler.Compiler;
import at.dms.util.Utils;

public class CParseCompilationUnitContext implements DeepCloneable {
  public static CParseCompilationUnitContext getInstance() {
    return stack.size() == 0 ?
      new CParseCompilationUnitContext() :
      (CParseCompilationUnitContext)stack.pop();
  }

  public void release() {
    release(this);
  }

  public static void release(CParseCompilationUnitContext context) {
    context.clear();
    stack.push(context);
  }

  private CParseCompilationUnitContext() {
  }

  private void clear() {
    packageImports.setSize(0);
    classImports.setSize(0);
    typeDeclarations.setSize(0);
    pack = null;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (ADD)
  // ----------------------------------------------------------------------

  public void setPackage(JPackageName pack) {
    packageName = pack == JPackageName.UNNAMED ? "" : pack.getName() + '/';
    this.pack = pack;
  }

  public void addPackageImport(JPackageImport pack) {
    packageImports.addElement(pack);
  }

  public void addClassImport(JClassImport clazz) {
    classImports.addElement(clazz);
  }

  public void addTypeDeclaration(JTypeDeclaration decl) {
    typeDeclarations.addElement(decl);
    decl.generateInterface(null, packageName);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (GET)
  // ----------------------------------------------------------------------

  public JPackageImport[] getPackageImports() {
    return (JPackageImport[])Utils.toArray(packageImports,
					   JPackageImport.class);
  }

  public JClassImport[] getClassImports() {
    return (JClassImport[])Utils.toArray(classImports,
					 JClassImport.class);
  }

  public JTypeDeclaration[] getTypeDeclarations() {
    return (JTypeDeclaration[])Utils.toArray(typeDeclarations,
					     JTypeDeclaration.class);
  }

  public JPackageName getPackageName() {
    return pack;
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JPackageName		pack;
  private String		packageName;
  private Vector		packageImports = new Vector();
  private Vector		classImports = new Vector();
  private Vector		typeDeclarations = new Vector();

  private static Stack		stack = new Stack();

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CParseCompilationUnitContext other = new at.dms.kjc.CParseCompilationUnitContext();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CParseCompilationUnitContext other) {
  other.pack = (at.dms.kjc.JPackageName)at.dms.kjc.AutoCloner.cloneToplevel(this.pack);
  other.packageName = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.packageName);
  other.packageImports = (java.util.Vector)at.dms.kjc.AutoCloner.cloneToplevel(this.packageImports);
  other.classImports = (java.util.Vector)at.dms.kjc.AutoCloner.cloneToplevel(this.classImports);
  other.typeDeclarations = (java.util.Vector)at.dms.kjc.AutoCloner.cloneToplevel(this.typeDeclarations);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
