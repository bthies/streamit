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
 * $Id: JExpressionListStatement.java,v 1.8 2003-08-29 19:25:36 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.JavaStyleComment;

/**
 * JLS 14.8: Expression Statement
 *
 * Certain kinds of expressions may be used as statements by following them with semicolon.
 * An expression statement is executed by evaluating the expression; if the expression has
 * a value, the value is discarded.
 */
public class JExpressionListStatement extends JStatement {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JExpressionListStatement() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	exprs		the expressions to evaluate
   * @param	comments	comments in the source code
   */
  public JExpressionListStatement(TokenReference where,
				  JExpression[] exprs,
				  JavaStyleComment[] comments)
  {
    super(where, comments);
    this.exprs = exprs;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns an array of expression
   */
  public JExpression[] getExpressions() {
   return exprs;
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
    for (int i = 0; i < exprs.length; i++) {
      // the result of the expression will be discarded
      exprs[i] = exprs[i].analyse(new CExpressionContext(context, false, true));
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
    super.accept(p);
    p.visitExpressionListStatement(this, exprs);
  }

     /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
   return  p.visitExpressionListStatement(this, exprs);
  }
      

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    setLineNumber(code);

    for (int i = 0; i < exprs.length; i++) {
      exprs[i].genCode(code, true);
    }
  }

    /**
     * Returns the i'th expression
     */
    public JExpression getExpression(int i) {
	return exprs[i];
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JExpression[]		exprs;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JExpressionListStatement other = new at.dms.kjc.JExpressionListStatement();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JExpressionListStatement other) {
  super.deepCloneInto(other);
  other.exprs = (at.dms.kjc.JExpression[])at.dms.kjc.AutoCloner.cloneToplevel(this.exprs, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
