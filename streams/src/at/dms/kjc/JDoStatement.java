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
 * $Id: JDoStatement.java,v 1.8 2003-08-21 09:44:20 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.CWarning;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * JLS 14.12: Do Statement
 *
 * The do statement executes an expression and a statement repeatedly
 * until the value of the expression is false.
 */
public class JDoStatement extends JLoopStatement {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JDoStatement() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	cond		the expression to evaluate
   * @param	body		the loop body
   * @param	comments	comments in the source text
   */
  public JDoStatement(TokenReference where,
		      JExpression cond,
		      JStatement body,
		      JavaStyleComment[] comments)
  {
    super(where, comments);

    this.cond = cond;
    this.body = body;
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
    try {
      CLoopContext	loopContext;

      loopContext = new CLoopContext(context, this);

      body.analyse(loopContext);

      if (loopContext.isContinueTarget()) {
	if (loopContext.isReachable()) {
	  loopContext.merge(loopContext.getContinueContextSummary());
	} else {
	  loopContext.adopt(loopContext.getContinueContextSummary());
	  loopContext.setReachable(true);
	}
      }

      cond = cond.analyse(new CExpressionContext(loopContext));
      check(loopContext,
	    cond.getType() == CStdType.Boolean,
	    KjcMessages.DO_COND_NOTBOOLEAN, cond.getType());
      if (cond instanceof JAssignmentExpression) {
	loopContext.reportTrouble(new CWarning(getTokenReference(),
					       KjcMessages.ASSIGNMENT_IN_CONDITION));
      }

      loopContext.close(getTokenReference());

      if (cond.isConstant() && cond.booleanValue()) {
	context.setReachable(false);
      }

      if (loopContext.isBreakTarget()) {
	if (context.isReachable()) {
	  context.merge(loopContext.getBreakContextSummary());
	} else {
	  context.adopt(loopContext.getBreakContextSummary());
	}
      }
    } catch (CBlockError e) {
      context.reportTrouble(e);
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
    p.visitDoStatement(this, cond, body);
  }

     /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return p.visitDoStatement(this, cond, body);
  }

    /**
     * Sets the condition.
     */
    public void setCondition(JExpression cond) {
	this.cond = cond;
    }

    /**
     * Sets the body.
     */
    public void setBody(JStatement body) {
	this.body = body;
    }

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    setLineNumber(code);

    code.pushContext(this);

    CodeLabel		bodyLabel = new CodeLabel();

    code.plantLabel(bodyLabel);				//	body:
    body.genCode(code);					//		BODY CODE
    code.plantLabel(getContinueLabel());			//	cont:
    cond.genBranch(true, code, bodyLabel);		//		EXPR CODE; IFNE body
    code.plantLabel(getBreakLabel());				//	end:	...

    code.popContext(this);
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JExpression		cond;
  private JStatement		body;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JDoStatement other = new at.dms.kjc.JDoStatement();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JDoStatement other) {
  super.deepCloneInto(other);
  other.cond = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.cond, this);
  other.body = (at.dms.kjc.JStatement)at.dms.kjc.AutoCloner.cloneToplevel(this.body, this);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
