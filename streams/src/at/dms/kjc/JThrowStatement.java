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
 * $Id: JThrowStatement.java,v 1.4 2003-05-16 21:58:36 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.JavaStyleComment;

/**
 * JLS 14.17: Throw Statement
 *
 * A throw statement causes an exception to be thrown.
 */
public class JThrowStatement extends JStatement {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JThrowStatement() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	expr		the expression to throw.
   */
  public JThrowStatement(TokenReference where,
			 JExpression expr,
			 JavaStyleComment[] comments)
  {
    super(where, comments);
    this.expr = expr;
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
    expr = expr.analyse(new CExpressionContext(context));
    check(context,
	  expr.getType().isReference()
	  && expr.isAssignableTo(CStdType.Throwable),
	  KjcMessages.THROW_BADTYPE, expr.getType());

    if (expr.getType().isCheckedException()) {
      context.addThrowable(new CThrowableInfo((CClassType)expr.getType(), this));
    }
    context.setReachable(false);
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    super.accept(p);
    p.visitThrowStatement(this, expr);
  }

   /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
    public Object accept(AttributeVisitor p) {
	return p.visitThrowStatement(this, expr);
    }

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    setLineNumber(code);

    expr.genCode(code, false);
    code.plantNoArgInstruction(opc_athrow);
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JExpression		expr;
}
