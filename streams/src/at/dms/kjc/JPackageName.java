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
 * $Id: JPackageName.java,v 1.7 2003-08-29 19:25:37 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.TokenReference;
import at.dms.compiler.JavaStyleComment;

/**
 * This class represents the "package at.dms.kjc" statement
 */
public class JPackageName extends JPhylum {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JPackageName() {} // for cloner only

  /**
   * construct a package name
   *
   * @param	where		the token reference of this node
   * @param	name		the package name
   */
  public JPackageName(TokenReference where, String name, JavaStyleComment[] comments) {
    super(where);

    this.name = name.intern();
    this.comments = comments;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns the package name defined by this declaration.
   *
   * @return	the package name defined by this declaration
   */
  public String getName() {
    return name;
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
    if (!name.equals("")) {
      p.visitPackageName(name.replace('/', '.'));
    }
  }

     /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
  if (comments != null) {
      return p.visitComments(comments);
  }
    if (!name.equals("")) {
     return  p.visitPackageName(name.replace('/', '.'));
    }
    return null;
  }
      

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  /**
   * The unnamed package (JLS 7.4.2).
   */
  public static final JPackageName	UNNAMED = new JPackageName(TokenReference.NO_REF, "", null);

    private /* final */ String			name; // removed final for cloner
    private /* final */ JavaStyleComment[]	comments; // removed final for cloner

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JPackageName other = new at.dms.kjc.JPackageName();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JPackageName other) {
  super.deepCloneInto(other);
  other.name = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.name, other);
  other.comments = (at.dms.compiler.JavaStyleComment[])at.dms.kjc.AutoCloner.cloneToplevel(this.comments, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
