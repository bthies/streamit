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
 * $Id: JIfStatement.java,v 1.8 2003-05-28 05:58:43 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.CWarning;
import at.dms.compiler.TokenReference;
import at.dms.compiler.JavaStyleComment;

/**
 * JLS 14.9: If Statement
 *
 * The if statement executes an expression and a statement repeatedly
 * until the value of the expression is false.
 */
public class JIfStatement extends JStatement {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JIfStatement() {} // for cloner only
  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	cond		the expression to evaluate
   * @param	thenClause	the statement to execute if the condition is true
   * @param	elseClause	the statement to execute if the condition is false
   */
  public JIfStatement(TokenReference where,
		      JExpression cond,
		      JStatement thenClause,
		      JStatement elseClause,
		      JavaStyleComment[] comments)
  {
    super(where, comments);

    this.cond = cond;
    this.thenClause = thenClause;
    this.elseClause = elseClause;
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
    cond = cond.analyse(new CExpressionContext(context));
    check(context,
	  cond.getType() == CStdType.Boolean,
	  KjcMessages.IF_COND_NOTBOOLEAN, cond.getType());
    if (cond instanceof JAssignmentExpression) {
      context.reportTrouble(new CWarning(getTokenReference(),
					 KjcMessages.ASSIGNMENT_IN_CONDITION));
    }

    CBodyContext	thenContext = new CSimpleBodyContext(context, context);

    thenClause.analyse(thenContext);

    if (elseClause == null) {
      context.merge(thenContext);
    } else {
      elseClause.analyse(context);
      if (thenContext.isReachable() && context.isReachable()) {
	context.merge(thenContext);
      } else if (thenContext.isReachable()) {
	context.adopt(thenContext);
      }
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
    p.visitIfStatement(this, cond, thenClause, elseClause);
  }

     /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
    return p.visitIfStatement(this, cond, thenClause, elseClause);
  }        

    /**
     * Sets condition of this.
     */
    public void setCondition(JExpression cond) {
	this.cond = cond;
    }

    /**
     * Gets condition of this.
     */
    public JExpression getCondition() {
	return cond;
    }

    /**
     * Set then clause.
     */
    public void setThenClause(JStatement thenClause) {
	this.thenClause = thenClause;
    }

    /**
     * Get then clause.
     */
    public JStatement getThenClause() {
	return thenClause;
    }

    /**
     * Get else clause.
     */
    public JStatement getElseClause() {
	return elseClause;
    }

    /**
     * Set else clause.
     */
    public void setElseClause(JStatement elseClause) {
	this.elseClause = elseClause;
    }


  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    setLineNumber(code);

    if (cond.isConstant()) {
      if (cond.booleanValue()) {
	thenClause.genCode(code);
      } else if (elseClause != null) {
	elseClause.genCode(code);
      }
    } else {
      CodeLabel		elseLabel = new CodeLabel();
      CodeLabel		nextLabel = new CodeLabel();

      cond.genBranch(false, code, elseLabel);   //		COND IFEQ else
      thenClause.genCode(code);			//		THEN CODE
      code.plantJumpInstruction(opc_goto, nextLabel);	//		GOTO next
      code.plantLabel(elseLabel);		//	else:
      if (elseClause != null) {
	elseClause.genCode(code);		//		ELSE CODE
      }
      code.plantLabel(nextLabel);		//	next	...
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JExpression		cond;
  private JStatement		thenClause;
  private JStatement		elseClause;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JIfStatement other = new at.dms.kjc.JIfStatement();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JIfStatement other) {
  super.deepCloneInto(other);
  other.cond = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.cond);
  other.thenClause = (at.dms.kjc.JStatement)at.dms.kjc.AutoCloner.cloneToplevel(this.thenClause);
  other.elseClause = (at.dms.kjc.JStatement)at.dms.kjc.AutoCloner.cloneToplevel(this.elseClause);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
