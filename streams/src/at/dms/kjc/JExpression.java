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
 * $Id: JExpression.java,v 1.6 2003-05-28 05:58:43 thies Exp $
 */ 
 
package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
import at.dms.util.InconsistencyException;
import at.dms.util.MessageDescription;

/**
 * Root class for all expressions
 */
public abstract class JExpression extends JPhylum {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JExpression() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   */
  public JExpression(TokenReference where) {
    super(where);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns the type of this expression (call after parsing only)
   */
  public abstract CType getType();

  /**
   * Tests whether this expression denotes a compile-time constant (JLS 15.28).
   *
   * @return	true iff this expression is constant
   */
  public boolean isConstant() {
    return false;
  }

  /**
   * Returns true iff this expression can be used as a statement (JLS 14.8)
   */
  public boolean isStatementExpression() {
    return false;
  }

  // ----------------------------------------------------------------------
  // LITERAL HANDLING
  // ----------------------------------------------------------------------

  /**
   * Returns the literal value of this field
   */
  public JLiteral getLiteral() {
    throw new InconsistencyException(this + " is not a literal");
  }

  /**
   * Returns the constant value of the expression.
   * The expression must be a literal.
   */
  public boolean booleanValue() {
    throw new InconsistencyException(this + " is not a boolean literal");
  }

  /**
   * Returns the constant value of the expression.
   * The expression must be a literal.
   */
  public byte byteValue() {
    throw new InconsistencyException(this + " is not a byte literal");
  }

  /**
   * Returns the constant value of the expression.
   * The expression must be a literal.
   */
  public char charValue() {
    throw new InconsistencyException(this + " is not a char literal");
  }

  /**
   * Returns the constant value of the expression.
   * The expression must be a literal.
   */
  public double doubleValue() {
    throw new InconsistencyException(this + " is not a double literal");
  }

  /**
   * Returns the constant value of the expression.
   * The expression must be a literal.
   */
  public float floatValue() {
    throw new InconsistencyException(this + " is not a float literal");
  }

  /**
   * Returns the constant value of the expression.
   * The expression must be a literal.
   */
  public int intValue() {
    throw new InconsistencyException(this + " is not an int literal");
  }

  /**
   * Returns the constant value of the expression.
   * The expression must be a literal.
   */
  public long longValue() {
    throw new InconsistencyException(this + " is not a long literal");
  }

  /**
   * Returns the constant value of the expression.
   * The expression must be a literal.
   */
  public short shortValue() {
    throw new InconsistencyException(this + " is not a short literal");
  }

  /**
   * Returns the constant value of the expression.
   * The expression must be a literal.
   */
  public String stringValue() {
    throw new InconsistencyException(this + " is not a string literal");
  }

  // ----------------------------------------------------------------------
  // ASSIGNMENT
  // ----------------------------------------------------------------------

  /**
   * Tests whether this expression can be at the left-hand side of
   * an assignment, i.e. denotes a variable at call time.
   *
   * Note : a final variable is an l-value until it is initialized.
   *
   * @return	true iff this expression is an l-value
   */
  public boolean isLValue(CExpressionContext context) {
    return false;
  }

  /**
   * Tests whether this expression is final, like a variable, which is 
   * final. Used in loops to intentify assignments to final vaiables.
   *
   * @return	true iff this expression is final
   */
  public boolean isFinal() {
    return false;
  }
  /**
   * Used in field access expressions, local variable expression ...
   *
   * @return	the intentifier
   */
  public String getIdent() {
    throw new InconsistencyException(this + " is not an l-value");
  }

  /**
   * Declares this variable to be initialized.
   *
   * @exception	UnpositionedError an error if this object can't actually
   *		be assignated this may happen with final variables.
   */
  public void setInitialized(CExpressionContext context)
    throws UnpositionedError
  {
    throw new InconsistencyException(this + " is not an l-value");
  }

  /**
   * @return	true if this expression is a variable already valued
   */
  public boolean isInitialized(CExpressionContext context) {
    throw new InconsistencyException(this + " is not an l-value");
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
  public abstract JExpression analyse(CExpressionContext context)
    throws PositionedError;

  /**
   * Adds a compiler error.
   * @param	context		the context in which the error occurred
   * @param	key		the message ident to be displayed
   * @param	params		the array of parameters
   *
   */
  protected void fail(CContext context, MessageDescription key, Object[] params)
    throws PositionedError
  {
    throw new CLineError(getTokenReference(), key, params);
  }

    /**
     * Analyse without a null context.
     */
    public JExpression convertType(CType dest) {
	try {
	    return convertType(dest, null);
	} catch (PositionedError e) {
	    e.printStackTrace();
	    return null;
	}
    }

  /**
   * convertType
   * changes the type of this expression to an other
   * @param  dest the destination type
   */
  public JExpression convertType(CType dest, CExpressionContext context)
    throws PositionedError
  {
    if (getType().equals(dest)) {
      return this;
    } else {
      return new JUnaryPromote(this, dest).analyse(context);
    }
  }

  /**
   * Can this expression be converted to the specified type by
   * assignment conversion (JLS 5.2) ?
   * @param	dest		the destination type
   * @return	true iff the conversion is valid
   */
  public boolean isAssignableTo(CType dest) {
      return getType().isAssignableTo(dest);
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
    public abstract void accept(KjcVisitor p);
  /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
    public abstract Object accept(AttributeVisitor p);

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the code list
   * @param	discardValue	discard the result of the evaluation ?
   */
  public abstract void genCode(CodeSequence code, boolean discardValue);

  /**
   * Generates a sequence of bytescodes to branch on a label
   * This method helps to handle heavy optimizables conditions
   * @param	code		the code list
   */
  public void genBranch(boolean cond, CodeSequence code, CodeLabel label) {
    genCode(code, false);
    code.plantJumpInstruction(cond ? opc_ifne : opc_ifeq, label);
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
    throw new InconsistencyException(this + " is not an l-value");
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
    throw new InconsistencyException(this + " is not an l-value");
  }

  // ----------------------------------------------------------------------
  // PUBLIC CONSTANTS
  // ----------------------------------------------------------------------

  public static final JExpression[]		EMPTY = new JExpression[0];

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JExpression other) {
  super.deepCloneInto(other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
