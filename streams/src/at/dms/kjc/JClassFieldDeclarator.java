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
 * $Id: JClassFieldDeclarator.java,v 1.4 2003-05-28 05:58:43 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * JLS 8.3 : Class Field Declaration.
 * JLS 9.3 ; Field (Constant) Declaration.
 *
 */
public class JClassFieldDeclarator extends JStatement {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JClassFieldDeclarator() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	decl		the declarator of this field
   */
  public JClassFieldDeclarator(TokenReference where, JFieldDeclaration decl) {
    super(where, null);
    this.decl = decl;
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Analyses the statement (semantically).
   * @param	context		the analysis context
   * @exception	PositionedError	the analysis detected an error
   */
  public void analyse(CBodyContext context) throws PositionedError {
    decl.analyse(context);

    if (decl.hasInitializer()) {
      ((CSourceField)decl.getField()).setValue(decl.getVariable().getValue());
    }
  }

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    // utility class for classfile
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    null;   // utility class for classfile
  }

  /**
   * Generates a sequence of bytescodes
   *
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    if (decl.getField().getConstantValue() == null) {
      decl.genCode(code);
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JFieldDeclaration	decl;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JClassFieldDeclarator other = new at.dms.kjc.JClassFieldDeclarator();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JClassFieldDeclarator other) {
  super.deepCloneInto(other);
  other.decl = (at.dms.kjc.JFieldDeclaration)at.dms.kjc.AutoCloner.cloneToplevel(this.decl);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
