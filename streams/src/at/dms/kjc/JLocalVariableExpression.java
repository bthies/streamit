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
 * $Id: JLocalVariableExpression.java,v 1.7 2003-08-21 09:44:20 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * Root class for all expressions
 */
public class JLocalVariableExpression extends JExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JLocalVariableExpression() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param where the line of this node in the source code
   */
  public JLocalVariableExpression(TokenReference where, JLocalVariable variable) {
    super(where);
    if (variable == null)
	throw new RuntimeException();
    this.variable = variable;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns true if this field accept assignment
   */
  public boolean isLValue(CExpressionContext context) {
    return !variable.isFinal() || !myBeInitialized(context);
  }

  /**
   * Returns true if there must be exactly one initialization of the variable. 
   *
   * @return true if the variable is final.
   */
  public boolean isFinal() {
    return variable.isFinal();
  }
 
  /**
   * Returns true if this field is already initialized
   */
  public boolean isInitialized(CExpressionContext context) {
    return CVariableInfo.isInitialized(context.getBodyContext().getVariableInfo(variable.getIndex()));
  }

  /**
   * Returns true if this field may be initialized (used for assignment) 
   */
  public boolean myBeInitialized(CExpressionContext context) {
    return CVariableInfo.mayBeInitialized(context.getBodyContext().getVariableInfo(variable.getIndex()));
  }

  /**
   * Declares this variable to be initialized.
   *
   */
  public void setInitialized(CExpressionContext context) {
    context.getBodyContext().setVariableInfo(variable.getIndex(), CVariableInfo.INITIALIZED);
  }

  /**
   * Returns the position of this variable in the sets of local vars
   */
  public int getPosition() {
    return variable.getPosition();
  }

  /**
   * Compute the type of this expression (called after parsing)
   * @return the type of this expression
   */
  public CType getType() {
    return variable.getType();
  }

  public String getIdent() {
    return variable.getIdent();
  }

  /**
   * Tests whether this expression denotes a compile-time constant (JLS 15.28).
   *
   * @return	true iff this expression is constant
   */
  public boolean isConstant() {
    return variable.isConstant();
  }

  /**
   * Returns the literal value of this field
   */
  public JLiteral getLiteral() {
    return (JLiteral)variable.getValue();
  }

  public JLocalVariable getVariable() {
    return variable;
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
    if (!context.isLeftSide() || !context.discardValue()) {
      variable.setUsed();
    }
    if (context.isLeftSide()) {
      variable.setAssigned(getTokenReference(), context.getBodyContext());
    }

    check(context,
	  CVariableInfo.isInitialized(context.getBodyContext().getVariableInfo(variable.getIndex()))
	  || (context.isLeftSide() && context.discardValue()),
	  KjcMessages.UNINITIALIZED_LOCAL_VARIABLE, variable.getIdent());

    if (variable.isConstant() && !context.isLeftSide()) {
      return variable.getValue();
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
      p.visitLocalVariableExpression(this, variable.getIdent());
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitLocalVariableExpression(this, variable.getIdent());
  }

  public boolean equals(Object o) {
    return (o instanceof JLocalVariableExpression) &&
      variable.equals(((JLocalVariableExpression)o).variable);
  }

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    if (! discardValue) {
      setLineNumber(code);
      variable.genLoad(code);
    }
  }

  /**
   * Generates JVM bytecode to store a value into the storage location
   * denoted by this expression.
   *
   * Storing is done in 3 steps :
   * - prefix code for the storage location (may be empty),
   * - code to determine the value to store,
   * - suffix code for the storage location.
   *
   * @param	code		the code list
   */
  public void genStartStoreCode(CodeSequence code) {
    // nothing to do here
  }

  /**
   * Generates JVM bytecode to store a value into the storage location
   * denoted by this expression.
   *
   * Storing is done in 3 steps :
   * - prefix code for the storage location (may be empty),
   * - code to determine the value to store,
   * - suffix code for the storage location.
   *
   * @param	code		the code list
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genEndStoreCode(CodeSequence code, boolean discardValue) {
    if (!discardValue) {
      int	opcode;

      if (getType().getSize() == 2) {
	opcode = opc_dup2;
      } else {
	opcode = opc_dup;
      }
      code.plantNoArgInstruction(opcode);
    }
    variable.genStore(code);
  }

    public String toString() {
	return "VarExp:"+variable.getIdent();
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  JLocalVariable	variable;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JLocalVariableExpression other = new at.dms.kjc.JLocalVariableExpression();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JLocalVariableExpression other) {
  super.deepCloneInto(other);
  other.variable = (at.dms.kjc.JLocalVariable)at.dms.kjc.AutoCloner.cloneToplevel(this.variable, this);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
