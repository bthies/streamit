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
 * $Id: JInstanceofExpression.java,v 1.3 2003-05-16 21:58:35 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.CWarning;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;

/**
 * This class represents a instanceof expression.
 */
public class JInstanceofExpression extends JExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JInstanceofExpression() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   *
   * @param	where	the line of this node in the source code
   * @param	expr	the expression to be casted
   * @param	dest	the type to test for
   */
  public JInstanceofExpression(TokenReference where, JExpression expr, CType dest) {
    super(where);
    this.expr = expr;
    this.dest = dest;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Compute the type of this expression.
   *
   * @return the type of this expression
   */
  public CType getType() {
    return CStdType.Boolean;
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Analyses the expression (semantically).
   * @param	context		the analysis context
   * @return	an equivalent, analysed expression
   * @exception	PositionedError	the analysis detected an error
   */
  public JExpression analyse(CExpressionContext context) throws PositionedError {
    expr = expr.analyse(context);
    try {
      dest.checkType(context);
    } catch (UnpositionedError e) {
      throw e.addPosition(getTokenReference());
    }
    check(context, expr.getType().isReference(),
	  KjcMessages.INSTANCEOF_BADTYPE, expr.getType(), dest);

    check(context, dest.isCastableTo(expr.getType()),
	  KjcMessages.INSTANCEOF_BADTYPE, expr.getType(), dest);

    if (expr.getType().isAssignableTo(dest)) {
      context.reportTrouble(new CWarning(getTokenReference(), KjcMessages.UNNECESSARY_INSTANCEOF, null));
    }

    return this;
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    p.visitInstanceofExpression(this, expr, dest);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitInstanceofExpression(this, expr, dest);
  }

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    setLineNumber(code);

    expr.genCode(code, false);
    code.plantClassRefInstruction(opc_instanceof, ((CClassType)dest).getQualifiedName());

    if (discardValue) {
      code.plantPopInstruction(getType());
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JExpression		expr;
  private CType			dest;
}
