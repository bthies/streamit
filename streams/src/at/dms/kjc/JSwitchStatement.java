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
 * $Id: JSwitchStatement.java,v 1.8 2003-08-29 19:25:37 thies Exp $
 */

package at.dms.kjc;

import java.util.Vector;

import at.dms.classfile.SwitchInstruction;
import at.dms.compiler.CWarning;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.JavaStyleComment;

/**
 * JLS 14.10: Switch Statement
 */
public class JSwitchStatement extends JStatement {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JSwitchStatement() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	expr		the expr part
   * @param	groups		the different part of body.
   */
  public JSwitchStatement(TokenReference where,
			  JExpression expr,
			  JSwitchGroup[] groups,
			  JavaStyleComment[] comments)
  {
    super(where, comments);

    this.expr = expr;
    this.groups = groups;
    this.hasBreak = false;
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
    // !!! graf 010109:
    // The type of the Expression must be char, byte, short, or int, or a compile-time error occurs.
    check(context, expr.getType().isAssignableTo(CStdType.Integer), KjcMessages.SWITCH_BADEXPR);

    CSwitchBodyContext		self = new CSwitchBodyContext(context, this);
    CSwitchGroupContext[]	groupContexts = new CSwitchGroupContext[groups.length];

    for (int i = 0; i < groups.length; i++) {
      if (i == 0 || !groupContexts[i - 1].isReachable()) {
	groupContexts[i] = new CSwitchGroupContext(self);
      } else {
	groupContexts[i] = groupContexts[i - 1];
      }
      groups[i].analyse(groupContexts[i]);
    }

    boolean	isFirst = self.defaultExists();

    for (int i = 0; i < groupContexts.length; i++) {
      if (isFirst && (groupContexts[i].isReachable() || groupContexts[i].isBreaked())) {
	self.adopt(groupContexts[i]);
	isFirst = false;
      } else if (groupContexts[i].isBreaked() ||
		 i == groupContexts.length - 1 && groupContexts[i].isReachable()) {
	self.merge(groupContexts[i]);
      }

      if (groupContexts[i].isReachable() && i != groupContexts.length - 1) {
	context.reportTrouble(new CWarning(getTokenReference(),
					   KjcMessages.CASE_FALL_THROUGH));
      }
    }
    self.setReachable(groupContexts.length == 0
		      || groupContexts[groupContexts.length - 1].isReachable()
		      || hasBreak
		      || !self.defaultExists());
    self.close(getTokenReference());

    if (!self.defaultExists()) {
      context.reportTrouble(new CWarning(getTokenReference(),
					 KjcMessages.SWITCH_NO_DEFAULT));
    }
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns the type of the switch expression.
   */
  /*package*/ CType getType() {
    return expr.getType();
  }

  /**
   * Prevent statement that there is at least one break
   */
  public void addBreak(CBodyContext context) {
    hasBreak = true;
  }

  /**
   * Return the end of this block (for break statement)
   */
  public CodeLabel getBreakLabel() {
    return endLabel;
  }

    /**
     * Set the expression in this.
     */
    public void setExpression(JExpression expr) {
	this.expr = expr;
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
    p.visitSwitchStatement(this, expr, groups);
  }

     /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return p.visitSwitchStatement(this, expr, groups);
  }

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    setLineNumber(code);

    CodeLabel		defaultLabel = new CodeLabel();

    Vector		matches = new Vector();
    Vector		targets = new Vector();

    for (int i = 0; i < groups.length; i++) {
      groups[i].collectLabels(defaultLabel, matches, targets);
    }

    expr.genCode(code, false);

    code.pushContext(this);
    code.plantInstruction(new SwitchInstruction(defaultLabel, matches, targets));
    for (int i = 0; i < groups.length; i++) {
      groups[i].genCode(code);
    }

    if (!defaultLabel.hasAddress()) {
      code.plantLabel(defaultLabel);
    }

    code.plantLabel(endLabel);
    code.popContext(this);

    endLabel = null;
  }


  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JExpression		expr;
  private JSwitchGroup[]	groups;
  private boolean		hasBreak;
  private CodeLabel		endLabel = new CodeLabel();

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JSwitchStatement other = new at.dms.kjc.JSwitchStatement();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JSwitchStatement other) {
  super.deepCloneInto(other);
  other.expr = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.expr, other);
  other.groups = (at.dms.kjc.JSwitchGroup[])at.dms.kjc.AutoCloner.cloneToplevel(this.groups, other);
  other.hasBreak = this.hasBreak;
  other.endLabel = (at.dms.kjc.CodeLabel)at.dms.kjc.AutoCloner.cloneToplevel(this.endLabel, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
