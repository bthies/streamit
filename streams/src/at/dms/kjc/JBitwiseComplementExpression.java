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
 * $Id: JBitwiseComplementExpression.java,v 1.3 2003-05-16 21:58:35 thies Exp $
 */

package at.dms.kjc;

import at.dms.classfile.PushLiteralInstruction;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.util.InconsistencyException;

/**
 * JLS 15.15.5 Bitwise Complement Operator ~
 */
public class JBitwiseComplementExpression extends JUnaryExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JBitwiseComplementExpression() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	expr		the operand
   */
  public JBitwiseComplementExpression(TokenReference where, JExpression expr) {
    super(where, expr);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

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
    check(context, expr.getType().isOrdinal(), KjcMessages.UNARY_BADTYPE_BNOT, expr.getType());
    type = CNumericType.unaryPromote(expr.getType());
    expr = expr.convertType(type, context);

    if (expr.isConstant()) {
      if (type == CStdType.Long) {
	expr = new JLongLiteral(getTokenReference(), ~expr.longValue());
      } else if (type == CStdType.Integer) {
	expr = new JIntLiteral(getTokenReference(), ~expr.intValue());
      } else {
	throw new InconsistencyException();
      }
      return expr;
    } else {
      return this;
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
    p.visitBitwiseComplementExpression(this, expr);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitBitwiseComplementExpression(this, expr);
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
    if (type == CStdType.Long) {
      code.plantInstruction(new PushLiteralInstruction((long)-1));
      code.plantNoArgInstruction(opc_lxor);
    } else {
      code.plantInstruction(new PushLiteralInstruction((int)-1));
      code.plantNoArgInstruction(opc_ixor);
    }

    if (discardValue) {
      code.plantPopInstruction(getType());
    }
  }
}
