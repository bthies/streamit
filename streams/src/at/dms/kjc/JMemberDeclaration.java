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
 * $Id: JMemberDeclaration.java,v 1.6 2003-08-21 09:44:20 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.TokenReference;
import at.dms.compiler.JavadocComment;
import at.dms.compiler.JavaStyleComment;


/**
 * This class represents a java class in the syntax tree
 */
public abstract class JMemberDeclaration extends JPhylum {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JMemberDeclaration() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	javadoc		java documentation comments
   * @param	comments	other comments in the source code
   */
  public JMemberDeclaration(TokenReference where,
			    JavadocComment javadoc,
			    JavaStyleComment[] comments)
  {
    super(where);
    this.comments = comments;
    this.javadoc = javadoc;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (INTERFACE)
  // ----------------------------------------------------------------------

  /**
   * Returns true if this member is deprecated
   */
  public boolean isDeprecated() {
    return javadoc != null && javadoc.isDeprecated();
  }

  /**
   * @return	the interface
   */
  public CField getField() {
    return export.getField();
  }

  /**
   * @return	the interface
   */
  public CMethod getMethod() {
    return export.getMethod();
  }

  /**
   * @return	the interface
   */
  public CClass getCClass() {
    return export.getCClass();
  }

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    genComments(p);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return genComments1(p);
  }

  /**
   * Generate the code in pure java form
   * It is useful to debug and tune compilation process
   * @param	p		the printwriter into the code is generated
   */
  public Object genComments1(AttributeVisitor p) {
    if (comments != null) {
      return p.visitComments(comments);
    }
    if (javadoc != null) {
      return p.visitJavadoc(javadoc);
    }
    return null;
  }
     /**
   * Generate the code in pure java form
   * It is useful to debug and tune compilation process
   * @param	p		the printwriter into the code is generated
   */
  public void genComments(KjcVisitor p) {
    if (comments != null) {
      p.visitComments(comments);
    }
    if (javadoc != null) {
      p.visitJavadoc(javadoc);
    }
  }

  // ----------------------------------------------------------------------
  // PROTECTED ACCESSORS
  // ----------------------------------------------------------------------

  protected void setInterface(CMember export) {
    this.export = export;
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private CMember			export;
    private /* final */ JavadocComment		javadoc;  // removed final for cloner
    private /* final */ JavaStyleComment[]	comments; // removed final for cloner

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JMemberDeclaration other) {
  super.deepCloneInto(other);
  other.export = (at.dms.kjc.CMember)at.dms.kjc.AutoCloner.cloneToplevel(this.export, this);
  other.javadoc = (at.dms.compiler.JavadocComment)at.dms.kjc.AutoCloner.cloneToplevel(this.javadoc, this);
  other.comments = (at.dms.compiler.JavaStyleComment[])at.dms.kjc.AutoCloner.cloneToplevel(this.comments, this);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
 
