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
 * $Id: JCompilationUnit.java,v 1.9 2004-01-28 16:55:35 dmaze Exp $
 */

package at.dms.kjc;

import java.util.Hashtable;
import java.util.Vector;
import java.io.File;

import at.dms.compiler.CWarning;
import at.dms.compiler.Compiler;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;

/**
 * This class represents a virtual file and is the main entry point in java grammar
 */
public class JCompilationUnit extends JPhylum {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JCompilationUnit() {} // for cloning only

  /**
   * Constructs a CompilationUnit with the specified top level context
   * @param	where		the position of this token
   */
  public JCompilationUnit(TokenReference where,
			  JPackageName packageName,
			  JPackageImport[] importedPackages,
			  JClassImport[] importedClasses,
			  JTypeDeclaration[] typeDeclarations)
  {
    super(where);
    this.packageName = packageName;
    this.importedPackages = importedPackages;
    this.importedClasses = importedClasses;
    this.typeDeclarations = typeDeclarations;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * @return the package name of this compilation unit
   */
  public String getPackageName() {
    assert packageName != null;
    return packageName.getName();
  }

  /**
   * @return	the name of the file associated with this compilation unit
   */
  public String getFileName() {
    return getTokenReference().getFile();
  }

  // ----------------------------------------------------------------------
  // INTERFACE CHECKING
  // ----------------------------------------------------------------------

  /**
   * Second pass (quick), check interface looks good
   * @exception	PositionedError	an error with reference to the source file
   */
  public void checkInterface(Compiler compiler) throws PositionedError {
    export = new CCompilationUnit(packageName.getName(),
				  importedClasses,
				  importedPackages,
				  allLoadedClasses);

    CCompilationUnitContext	context = new CCompilationUnitContext(compiler, export);

    // JLS 7.5.1 Single-Type-Import Declaration
    for (int i = 0; i < importedClasses.length; i++) {
      JClassImport	ic = importedClasses[i];

      CClassType	impClass = CClassType.lookup(ic.getQualifiedName());

      try {
	impClass.checkType(context);
      } catch (UnpositionedError e) {
	throw e.addPosition(getTokenReference());
      }

      Object		clazz = allLoadedClasses.put(ic.getSimpleName(), impClass);
      if (clazz != null) {
	// JLS 7.5.1 :
	// If two single-type-import declarations in the same compilation
	// unit attempt to import types with the same simple name, then a
	// compile-time error occurs, unless the two types are the same type,
	// in which case the duplicate declaration is ignored.
	if (impClass != clazz) {
	  compiler.reportTrouble(new PositionedError(getTokenReference(),
						     KjcMessages.DUPLICATE_TYPE_NAME,
						     impClass.getIdent()));
	} else {
	  compiler.reportTrouble(new CWarning(getTokenReference(), KjcMessages.DUPLICATE_CLASS_IMPORT, ic.getQualifiedName()));
	}
      }
    }

    // JLS 7.5.2 Type-Import-on-Demand Declaration
    // check uniquness of classes
    for (int i = 0; i < typeDeclarations.length ; i++) {
      CClass	object = typeDeclarations[i].getCClass();

      Object		clazz = allLoadedClasses.get(object.getIdent());

      if (clazz == null) {
	allLoadedClasses.put(object.getIdent(), object.getType());
      } else {
	if (clazz != object) {
	  compiler.reportTrouble(new PositionedError(getTokenReference(),
						     KjcMessages.DUPLICATE_TYPE_NAME,
						     object.getQualifiedName()));
	}
      }
    }

    for (int i = 0; i < typeDeclarations.length ; i++) {
      typeDeclarations[i].checkInterface(context);
    }
  }

  /**
   * Second pass (quick), check interface looks good
   * Exceptions are not allowed here, this pass is just a tuning
   * pass in order to create informations about exported elements
   * such as Classes, Interfaces, Methods, Constructors and Fields
   * @return	true iff sub tree is correct enought to check code
   * @exception	PositionedError	an error with reference to the source file
   */
  public void checkInitializers(Compiler compiler, Vector classes) throws PositionedError {
    CCompilationUnitContext	context = new CCompilationUnitContext(compiler, export, classes);
    for (int i = 0; i < typeDeclarations.length ; i++) {
      typeDeclarations[i].checkInitializers(context);
    }
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Check expression and evaluate and alter context
   * @exception	PositionedError Error catched as soon as possible
   */
  public void checkBody(Compiler compiler, Vector classes) throws PositionedError {
    CCompilationUnitContext	context = new CCompilationUnitContext(compiler, export, classes);

    if (packageName == JPackageName.UNNAMED) {
      compiler.reportTrouble(new CWarning(getTokenReference(),
					  KjcMessages.PACKAGE_IS_MISSING));
    }

    for (int i = 0; i < typeDeclarations.length ; i++) {
      typeDeclarations[i].checkTypeBody(context);
    }

    // Check for unused class imports
    for (int i = 0; i < importedClasses.length; i++) {
      importedClasses[i].analyse(compiler);
    }
    // Check for unused package imports
    for (int i = 0; i < importedPackages.length; i++) {
      importedPackages[i].analyse(compiler, packageName);
    }
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Generate the code in pure java form
   * It is useful to debug and tune compilation process
   */
  public void accept(Main compiler, String destination) {
    KjcPrettyPrinter  pp;

    if (destination == null || destination.equals("")) {
      accept(pp = compiler.getPrettyPrinter(getTokenReference().getName() + ".gen"));
    } else {
      accept(pp = compiler.getPrettyPrinter(destination + File.separatorChar + getTokenReference().getName()));
    }
    pp.close();
  }

    

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    p.visitCompilationUnit(this, packageName, importedPackages, importedClasses, typeDeclarations);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitCompilationUnit(this, packageName, importedPackages, importedClasses, typeDeclarations);
  }

    public JTypeDeclaration[] getTypeDeclarations() {
	return typeDeclarations;
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JPackageName			packageName;
  private JClassImport[]		importedClasses;
  private JPackageImport[]		importedPackages;
  private JTypeDeclaration[]		typeDeclarations;

  private Hashtable			allLoadedClasses = new Hashtable(); // $$$ DEFAULT VALUE IS OKAY ???
  private CCompilationUnit		export;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JCompilationUnit other = new at.dms.kjc.JCompilationUnit();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JCompilationUnit other) {
  super.deepCloneInto(other);
  other.packageName = (at.dms.kjc.JPackageName)at.dms.kjc.AutoCloner.cloneToplevel(this.packageName);
  other.importedClasses = (at.dms.kjc.JClassImport[])at.dms.kjc.AutoCloner.cloneToplevel(this.importedClasses);
  other.importedPackages = (at.dms.kjc.JPackageImport[])at.dms.kjc.AutoCloner.cloneToplevel(this.importedPackages);
  other.typeDeclarations = (at.dms.kjc.JTypeDeclaration[])at.dms.kjc.AutoCloner.cloneToplevel(this.typeDeclarations);
  other.allLoadedClasses = (java.util.Hashtable)at.dms.kjc.AutoCloner.cloneToplevel(this.allLoadedClasses);
  other.export = (at.dms.kjc.CCompilationUnit)at.dms.kjc.AutoCloner.cloneToplevel(this.export);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
