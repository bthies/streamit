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
 * $Id: JPackageImport.java,v 1.4 2003-05-16 21:58:35 thies Exp $
 */

package at.dms.kjc;

import java.util.Vector;
import at.dms.compiler.Compiler;
import at.dms.compiler.CWarning;
import at.dms.compiler.TokenReference;
import at.dms.compiler.JavaStyleComment;

/**
 * This class represents the "package at.dms.kjc" statement
 */
public class JPackageImport extends JPhylum {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JPackageImport() {} // for cloner only

  /**
   * construct a package name
   *
   * @param	where		the token reference of this node
   * @param	name		the package name
   */
  public JPackageImport(TokenReference where, String name, JavaStyleComment[] comments) {
    super(where);

    this.name = name;
    this.comments = comments;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS & MUTATORS
  // ----------------------------------------------------------------------

  /**
   * Returns the package name defined by this declaration.
   *
   * @return	the package name defined by this declaration
   */
  public String getName() {
    return name;
  }

  /**
   * States that specified class in imported package is used.
   * @param	clazz		the class that is used.
   */
  public void setClassUsed(String clazz) {
    if (classesUsed == null) {
      classesUsed = new Vector();
    }
    classesUsed.addElement(clazz);
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Analyses the node (semantically).
   * @param	context		the analysis context
   * @param	thisPackage	the package name of the compilation unit
   * @exception	PositionedError	the analysis detected an error
   */
  public void analyse(Compiler compiler, JPackageName thisPackage) {
    if (getTokenReference() != TokenReference.NO_REF) {
      if (classesUsed == null) {
	compiler.reportTrouble(new CWarning(getTokenReference(),
					    KjcMessages.UNUSED_PACKAGE_IMPORT,
					    name.replace('/', '.'),
					    null));
      } else if (classesUsed.size() < 5) {
	StringBuffer	buffer = new StringBuffer();

	for (int i = 0; i < classesUsed.size(); i++) {
	  if (i != 0) {
	    buffer.append(", ");
	  }
	  buffer.append((name + "/" + (String)classesUsed.elementAt(i)).replace('/', '.'));
	}
	compiler.reportTrouble(new CWarning(getTokenReference(),
					    KjcMessages.REPLACE_PACKAGE_IMPORT,
					    name.replace('/', '.'),
					    buffer.toString()));
      }

      if (name.equals("java/lang")) {
	compiler.reportTrouble(new CWarning(getTokenReference(),
					    KjcMessages.IMPORT_JAVA_LANG,
					    name));
      }

      if (name.equals(thisPackage.getName())) {
	compiler.reportTrouble(new CWarning(getTokenReference(),
					    KjcMessages.IMPORT_CURRENT_PACKAGE,
					    name));
      }
    }
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    if (comments != null) {
      p.visitComments(comments);
    }
    p.visitPackageImport(name);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      if (comments != null) {
	  return p.visitComments(comments);
      }
      return p.visitPackageImport(name);
  }
      

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    private /* final */ String			name;  // removed final for cloner
    private /* final */ JavaStyleComment[]	comments;  // removed final for cloner
  private Vector			classesUsed;
}
