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
 * $Id: JBinaryExpression.java,v 1.9 2003-08-29 19:25:36 thies Exp $
 */

package at.dms.kjc;

import at.dms.classfile.PushLiteralInstruction;
import at.dms.compiler.TokenReference;

/**
 * This class is an abstract root class for binary expressions
 */
public abstract class JBinaryExpression extends JExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JBinaryExpression() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	p1		left operand
   * @param	p2		right operand
   */
  public JBinaryExpression(TokenReference where,
			   JExpression left,
			   JExpression right)
  {
    super(where);
    this.left = left;
    this.right = right;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * @return the type of this expression
   */
  public CType getType() {
    return type;
  }

    public void setType(CType t) {
	this.type = t;
    }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genBooleanResultCode(CodeSequence code, boolean discardValue) {
    setLineNumber(code);

    CodeLabel		okayLabel = new CodeLabel();
    CodeLabel		nextLabel = new CodeLabel();

    genBranch(true, code, okayLabel);		//		LEFT CODE IFNE okay
    code.plantInstruction(new PushLiteralInstruction(0)); //	FALSE
    code.plantJumpInstruction(opc_goto, nextLabel);	//		GOTO next
    code.plantLabel(okayLabel);			//	okay:
    code.plantInstruction(new PushLiteralInstruction(1)); //	TRUE
    code.plantLabel(nextLabel);			//	next	...

    if (discardValue) {
      //!!! CHECKME : optimize ???
      code.plantPopInstruction(CStdType.Boolean);
    }
  }

  public JExpression constantFolding() {
    return this;
  }

  /**
   * Generates a sequence of bytescodes to branch on a label
   * This method helps to handle heavy optimizables conditions
   * @param	code		the code list
   */
  public void genBranch(boolean cond, CodeSequence code, CodeLabel label) {
    genBranch(left, right, cond, code, label);
  }

  /**
   * Optimize a bi-conditional expression
   */
  protected void genBranch(JExpression left,
			   JExpression right,
			   boolean cond,
			   CodeSequence code,
			   CodeLabel label)
  {
    genCode(code, false);
    code.plantJumpInstruction(cond ? opc_ifne : opc_ifeq, label);
  }

    /**
     * Set what appears on the left of this.
     */
    public void setLeft(JExpression left) {
	this.left = left;
    }

    /**
     * Set what appears on the right of this.
     */
    public void setRight(JExpression right) {
	this.right = right;
    }

    /**
     * Returns what appears on left.
     */
    public JExpression getLeft() {
	return left;
    }

    /**
     * Returns what appears on right.
     */
    public JExpression getRight() {
	return right;
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  protected	CType			type;
  protected	JExpression		left;
  protected	JExpression		right;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JBinaryExpression other) {
  super.deepCloneInto(other);
  other.type = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.type, other);
  other.left = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.left, other);
  other.right = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.right, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
