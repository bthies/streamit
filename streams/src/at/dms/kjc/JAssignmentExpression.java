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
 * $Id: JAssignmentExpression.java,v 1.10 2003-09-13 05:17:36 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.CWarning;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;

/**
 * This class implements the assignment operation
 */
public class JAssignmentExpression extends JBinaryExpression {

    private JLocalVariableExpression copyVar;

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JAssignmentExpression() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	left		the left operand
   * @param	right		the right operand
   */
  public JAssignmentExpression(TokenReference where,
			       JExpression left,
			       JExpression right)
  {
    super(where, left, right);
  }

    public JLocalVariableExpression getCopyVar() {
	return copyVar;
    }

    public void setCopyVar(JLocalVariableExpression var) {
	copyVar=var;
    }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns true iff this expression can be used as a statement (JLS 14.8)
   */
  public boolean isStatementExpression() {
    return true;
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
    right = right.analyse(new CExpressionContext(context, false, false));
    if (right instanceof JTypeNameExpression) {
      check(context, false, KjcMessages.VAR_UNKNOWN, ((JTypeNameExpression)right).getQualifiedName());
    }


    CBodyContext bc = context.getBodyContext();
       
    left = left.analyse(new CExpressionContext(context, true, true));
    check(context, left.isLValue(context), KjcMessages.ASSIGNMENT_NOTLVALUE);
    /* This rejects legal java in case of translated Beamformer.str.  --bft
      check(context, !bc.isInLoop() || !left.isFinal(), KjcMessages.FINAL_IN_LOOP, left.getIdent());
    */

    try {
      left.setInitialized(context);
    } catch (UnpositionedError e) {
      throw e.addPosition(getTokenReference());
    }
 
    check(context,
	  right.isAssignableTo(left.getType()),
	  KjcMessages.ASSIGNMENT_BADTYPE, right.getType(), left.getType());
    if (left.equals(right)) {
      context.reportTrouble(new CWarning(getTokenReference(), KjcMessages.SELF_ASSIGNMENT));
    }

    type = left.getType();
    right = right.convertType(type, context);

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
    p.visitAssignmentExpression(this, left, right);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
    return p.visitAssignmentExpression(this, left, right);
 }

  /**
   * Returns a string representation of this object.
   */
  public String toString() {
    StringBuffer	buffer = new StringBuffer();

    buffer.append("JAssignmentExpression[");
    buffer.append(left.toString());
    buffer.append(", ");
    buffer.append(right.toString());
    buffer.append("]");
    return buffer.toString();
  }

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    setLineNumber(code);

    left.genStartStoreCode(code);
    right.genCode(code, false);
    left.genEndStoreCode(code, discardValue);
  }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JAssignmentExpression other = new at.dms.kjc.JAssignmentExpression();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JAssignmentExpression other) {
  super.deepCloneInto(other);
  other.copyVar = (at.dms.kjc.JLocalVariableExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.copyVar, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
