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
 * $Id: JFormalParameter.java,v 1.5 2003-05-16 21:58:35 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
import java.io.*;

/**
 * This class represents a parameter declaration in the syntax tree
 */
public class JFormalParameter extends JLocalVariable {
  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JFormalParameter() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	ident		the name of this variable
   * @param	initializer	the initializer
   */
  public JFormalParameter(TokenReference where,
			  int desc,
			  CType type,
			  String ident,
			  boolean isFinal) {
    super(where, isFinal ? ACC_FINAL : 0, desc, type, ident, null);
  }

  // ----------------------------------------------------------------------
  // INTERFACE CHECKING
  // ----------------------------------------------------------------------

  /**
   * Second pass (quick), check interface looks good
   * Exceptions are not allowed here, this pass is just a tuning
   * pass in order to create informations about exported elements
   * such as Classes, Interfaces, Methods, Constructors and Fields
   * sub classes must check modifiers and call checkInterface(super)
   * @return true iff sub tree is correct enought to check code
   */
  public CType checkInterface(CClassContext context) {
    try {
      type.checkType(context);
      return type;
    } catch (UnpositionedError cue) {
      context.reportTrouble(cue.addPosition(getTokenReference()));
      return CStdType.Object;
    }
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Analyses the node (semantically).
   * @param	context		the analysis context
   * @exception	PositionedError	the analysis detected an error
   */
  public void analyse(CBodyContext context) throws PositionedError {
    try {
      type.checkType(context);
    } catch (UnpositionedError e) {
      throw e.addPosition(getTokenReference());
    }
    try {
      context.getBlockContext().addVariable(this);
    } catch (UnpositionedError e) {
      throw e.addPosition(getTokenReference());
    }
    context.setVariableInfo(getIndex(), CVariableInfo.INITIALIZED);
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    p.visitFormalParameters(this, isFinal(), getType(), getIdent());
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitFormalParameters(this, isFinal(), getType(), getIdent());
  }

  // ----------------------------------------------------------------------
  // PUBLIC CONSTANTS
  // ----------------------------------------------------------------------

  public static final JFormalParameter[]	EMPTY = new JFormalParameter[0];
}
