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
 * $Id: JArrayAccessExpression.java,v 1.10 2003-05-28 05:58:42 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * 15.12 Array Access Expressions
 * This class implements an access through an array
 * constant values may be folded at compile time
 */
public class JArrayAccessExpression extends JExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JArrayAccessExpression() {} // for cloner only
  /**
   * This version is the original kopi one; tries to resolve type
   * automatically.  If you know the type, use the other constructor
   * since kopi's reconstruction isn't very good if the type goes
   * through fields, causing null types later on in the compiler.  --bft
   *
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	accessor	a natural integer
   */
  public JArrayAccessExpression(TokenReference where,
				JExpression prefix,
				JExpression accessor) {
      this(where, prefix, accessor, null);
      tryToResolveType();
  }

  public JArrayAccessExpression(TokenReference where,
				JExpression prefix,
				JExpression accessor,
				CType type)
  {
    super(where);

    this.prefix = prefix;
    this.accessor = accessor;
    this.type = type;
  }

    public String toString() {
	return "ArrayAccess:"+prefix+"["+accessor+"]";
    }
    
  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * @return	the type of this expression
   */
  public CType getType() {
    return type;
  }

  /**
   * @return the prefix of this expression
   */
  public JExpression getPrefix() {
    return prefix;
  }
    
  /**
   * @return the accessor of this expression
   */
  public JExpression getAccessor() {
    return accessor;
  }

  /**
   * @return	true if this expression is a variable already valued
   */
  public boolean isInitialized(CExpressionContext context) {
    // nothing to do in array access 15.12 Array Access Expressions
    return true;
  }

  /**
   * Declares this variable to be initialized.
   *
   * @exception	UnpositionedError an error if this object can't actually
   *		be assignated this may happen with final variables.
   */
  public void setInitialized(CExpressionContext context) {
    // nothing to do in array access 15.12 Array Access Expressions
  }

  /**
   *
   */
  public boolean isLValue(CExpressionContext context) {
    // nothing to do in array access 15.12 Array Access Expressions
    return true;
  }

  public String getIdent() {
    return ((CArrayType)prefix.getType()).getElementType()+"[]";
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
    // evaluate the accessor in rhs mode, result will be used
    accessor = accessor.analyse(new CExpressionContext(context));
    check(context,
	  accessor.getType().isAssignableTo(CStdType.Integer),
	  KjcMessages.ARRAY_EXPRESSION_INT, accessor.getType());

    // evaluate the prefix in rhs mode, result will be used
    prefix = prefix.analyse(new CExpressionContext(context));
    check(context, prefix.getType().isArrayType(), KjcMessages.ARRAY_PREFIX);

    type = ((CArrayType)prefix.getType()).getElementType();

    // no constant folding is applied to array access expressions

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
    p.visitArrayAccessExpression(this, prefix, accessor);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
    return p.visitArrayAccessExpression(this, prefix, accessor);
  }

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    setLineNumber(code);

    prefix.genCode(code, false);
    accessor.genCode(code, false);
    code.plantNoArgInstruction(type.getArrayLoadOpcode());

    if (discardValue) {
      code.plantPopInstruction(type);
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
    prefix.genCode(code, false);
    accessor.genCode(code, false);
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
      if (getType().getSize() == 2) {
	code.plantNoArgInstruction(opc_dup2_x2);
      } else if (type.getSize() == 1) {
	code.plantNoArgInstruction(opc_dup_x2);
      }
    }
    code.plantNoArgInstruction(type.getArrayStoreOpcode());
  }

    /**
     * Set the accessor of this.
     */
    public void setAccessor(JExpression a) {
	accessor = a;
    }
    
    public void setPrefix(JExpression p) {
	prefix = p;
    }

    /**
     * In the case when the prefix of this is a one-dimensional array
     * that is a local variable, resolves the type of this by looking
     * at the type of that variable.  This is for the StreamIt pass so
     * we can add references to array locals and have them be typed in
     * the IR.  A more general pass would call the top-level analyse()
     * function again, but this looked like it might be tricky.  Could
     * extend this function to be more sophisticated in the future.
     */
    private void tryToResolveType() {
	// get the var reference
	if (prefix instanceof JLocalVariableExpression) {
	    // get the variable type
	    CType type = 
		((JLocalVariableExpression)prefix).getVariable().getType();
	    // if we have an array type, set our type to the base type
	    if (type instanceof CArrayType) {
		this.type = ((CArrayType)type).getBaseType();
	    }
	}
    }

    // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JExpression		prefix;
  private JExpression		accessor;
  private CType			type;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JArrayAccessExpression other = new at.dms.kjc.JArrayAccessExpression();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JArrayAccessExpression other) {
  super.deepCloneInto(other);
  other.prefix = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.prefix);
  other.accessor = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.accessor);
  other.type = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.type);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
