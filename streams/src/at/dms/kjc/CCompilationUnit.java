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
 * $Id: CCompilationUnit.java,v 1.4 2003-05-16 21:58:34 thies Exp $
 */

package at.dms.kjc;

import java.util.Hashtable;

import at.dms.compiler.UnpositionedError;

/**
 * This class represents a compilation unit
 */
public class CCompilationUnit implements java.io.Serializable {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    private CCompilationUnit() {} // for cloner only

  /**
   * Construct a compilation unit context.
   */
  public CCompilationUnit(String packageName,
			  JClassImport[] importedClasses,
			  JPackageImport[] importedPackages,
			  Hashtable loadedClasses) {
    this.packageName = packageName;
    this.importedClasses = importedClasses;
    this.importedPackages = importedPackages;
    this.loadedClasses = loadedClasses;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (LOOKUP)
  // ----------------------------------------------------------------------

  /**
   * @param	caller		the class of the caller
   * @return	a class according to imports or null if error occur
   * @exception UnpositionedError	this error will be positioned soon
   */
  public CClassType lookupClass(CClass caller, String name) throws UnpositionedError {
    // $$$ USE A STRING BUFFER FOR IMPORT
    if (name.lastIndexOf('/') == -1) {
      // 6.5.4.1 Simple Type Names

      CClassType	cl;

      // First look for a type declared by a single-type-import of by a type declaration
      if ((cl = (CClassType)loadedClasses.get(name)) != null) {
	// If type is declared by a single-type-import, mark it as used (max. 1)
	for (int i = 0; i < importedClasses.length; i++) {
	  if (name == importedClasses[i].getSimpleName()) {
	    importedClasses[i].setUsed();
	    break;
	  }
	}

	return cl;
      }

      // Otherwise, look for a type declared in another compilation unit of this package
      if (packageName.length() == 0) {
	cl = CTopLevel.hasClassFile(name) ? CClassType.lookup(name) : null;
      } else {
	String		temp = packageName + '/' + name;

	cl = CTopLevel.hasClassFile(temp) ? CClassType.lookup(temp) : null;
      }

      if (cl != null) {
	loadedClasses.put(name, cl);
      } else {
	// Otherwise, look for a type declared by EXACTLY ONE import-on-demand declaration
	for (int i = 0; i < importedPackages.length; i++) {
	  String	qualifiedName = (importedPackages[i].getName() + '/' + name).intern();

	  if (CTopLevel.hasClassFile(qualifiedName)) {
	    CClassType	type = (CClassType)loadedClasses.get(name);

	    if (type != null && !type.getQualifiedName().equals(qualifiedName)) {
	      // Oops, the name is ambiguous (declared by more than one import-on-demand declaration)
	      throw new UnpositionedError(KjcMessages.CUNIT_RENAME2, name);
	    }
	    loadedClasses.put(name, CClassType.lookup(qualifiedName));
	    importedPackages[i].setClassUsed(name);
	  }
	}
      }

      // now the name must be unique and found
      if ((cl = (CClassType)loadedClasses.get(name)) == null) {
	throw new UnpositionedError(KjcMessages.CLASS_UNKNOWN, name);
      }

      return cl;
    } else {
      // 6.5.4.2 Qualified Type Names: look directly at top
      if (!CTopLevel.hasClassFile(name)) {
	throw new UnpositionedError(KjcMessages.CLASS_UNKNOWN, name);
      }

      return CClassType.lookup(name);
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    private /* final */ String			packageName; // removed final for cloner
    
    private /* final */ JClassImport[]		importedClasses; // removed final for cloner
    private /* final */ JPackageImport[]	importedPackages; // removed final for cloner
    
    private /* final */ Hashtable		loadedClasses; // removed final for cloner
}
