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
 * $Id: JClassExpression.java,v 1.6 2003-08-21 09:44:20 thies Exp $
 */

package at.dms.kjc;

import at.dms.classfile.PushLiteralInstruction;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
import at.dms.util.InconsistencyException;

/**
 * A 'int.class' expression
 */
public class JClassExpression extends JExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JClassExpression() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param where the line of this node in the source code
   */
  public JClassExpression(TokenReference where, CType type, int bounds) {
    super(where);
    this.type = type;
    this.prefix = null;
    this.bounds = bounds;
  }

  /**
   * Construct a node in the parsing tree
   * @param where the line of this node in the source code
   */
  public JClassExpression(TokenReference where, JExpression prefix, int bounds) {
    super(where);

    this.type = null;
    this.prefix = prefix;
    this.bounds = bounds;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Compute the type of this expression (called after parsing)
   * @return the type of this expression
   */
  public CType getType() {
    return CStdType.Class;
  }

  /**
   * Return the type this is a class expression for
   * @return the type this is a class expression for
   */
  public CType getClassType() {
    return this.type;
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
    if (prefix != null) {
      prefix = prefix.analyse(new CExpressionContext(context));
      check(context,
	    prefix instanceof JTypeNameExpression,
	    KjcMessages.CLASS_BAD_PREFIX, prefix);
      type = prefix.getType();
    }

    if (bounds > 0) {
      type = new CArrayType(type, bounds);
    }

    try {
      type.checkType(context);
    } catch (UnpositionedError e) {
      throw e.addPosition(getTokenReference());
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
    p.visitClassExpression(this, type);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitClassExpression(this, type);
  }

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    setLineNumber(code);

    String		className;

    switch (type.getTypeID()) {
    case TID_CLASS:
    case TID_ARRAY: // $$$ may be optimized and exception changed
      code.plantInstruction(new PushLiteralInstruction(((CClassType)type).getQualifiedName().replace('/', '.')));
      code.plantMethodRefInstruction(opc_invokestatic,
				     "java/lang/Class",
				     "forName",
				     "(Ljava/lang/String;)Ljava/lang/Class;");
      return;
    case TID_BOOLEAN:
      className = "java/lang/Boolean";
      break;
    case TID_BYTE:
      className = "java/lang/Byte";
      break;
    case TID_CHAR:
      className = "java/lang/Character";
      break;
    case TID_DOUBLE:
      className = "java/lang/Double";
      break;
    case TID_FLOAT:
      className = "java/lang/Float";
      break;
    case TID_INT:
      className = "java/lang/Integer";
      break;
    case TID_LONG:
      className = "java/lang/Long";
      break;
    case TID_SHORT:
      className = "java/lang/Short";
      break;
    case TID_VOID:
      className = "java/lang/Void";
      break;
    default:
      throw new InconsistencyException();
    }

    code.plantFieldRefInstruction(opc_getstatic,
				  className,
				  "TYPE",
				  CStdType.Class.getSignature());
    if (discardValue) {
      code.plantPopInstruction(type);
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private CType			type;
  private JExpression		prefix;
  private int			bounds;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JClassExpression other = new at.dms.kjc.JClassExpression();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JClassExpression other) {
  super.deepCloneInto(other);
  other.type = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.type, this);
  other.prefix = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.prefix, this);
  other.bounds = this.bounds;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
