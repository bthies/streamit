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
 * $Id: JInitializerDeclaration.java,v 1.5 2003-05-28 05:58:43 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * This class represents a java class in the syntax tree
 */
public class JInitializerDeclaration extends JMethodDeclaration {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JInitializerDeclaration() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	parent		parent in which this methodclass is built
   */
  public JInitializerDeclaration(TokenReference where,
				 JBlock body,
				 boolean isStatic,
				 boolean isDummy)
  {
    super(where,
	  ACC_PRIVATE | (isStatic ? ACC_STATIC : 0),
	  CStdType.Void,
	  (isStatic ? JAV_STATIC_INIT : JAV_INIT),
	  JFormalParameter.EMPTY,
	  CClassType.EMPTY,
	  body,
	  null,
	  null);

    this.isDummy = isDummy;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Return true if this initialiser declaration is used only to check code
   * and that it should not generate code
   */
  public boolean isDummy() {
    return isDummy;
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Analyses the node (semantically).
   * @param	context		the analysis context
   * @exception	PositionedError	the analysis detected an error
   */
  public void checkBody1(CClassContext context) {
    // I've made it !
    // nothing to check.
  }

  /**
   * FIXME: document
   * @param	context		the actual context of analyse
   * @exception	PositionedError		Error catched as soon as possible
   */
  public void checkInitializer(CClassContext context) throws PositionedError {
    if (!getMethod().isStatic() && !isDummy()) {
      context.addInitializer();
    }

    // we want to do that at check intitializers time
    CMethodContext	self = new CInitializerContext(context, getMethod());
    CBlockContext	block = new CBlockContext(self, parameters.length);

    if (!getMethod().isStatic()) {
      // add this local var
      block.addThisVariable();
    }

    body.analyse(block);

    block.close(getTokenReference());
    self.close(getTokenReference());
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    // Don t print initializers
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return null;    // Don t print initializers
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    private /* final */ boolean		isDummy; // removed final for cloner

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JInitializerDeclaration other = new at.dms.kjc.JInitializerDeclaration();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JInitializerDeclaration other) {
  super.deepCloneInto(other);
  other.isDummy = this.isDummy;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
