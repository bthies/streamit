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
 * $Id: JWhileStatement.java,v 1.7 2003-05-28 05:58:45 thies Exp $
 */ 

package at.dms.kjc;

import at.dms.compiler.CWarning;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * JLS 14.11: While Statement
 *
 * The while statement executes an expression and a statement repeatedly
 * until the value of the expression is false.
 */
public class JWhileStatement extends JLoopStatement {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JWhileStatement() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	cond		the expression to evaluate.
   * @param	body		the loop body.
   */
  public JWhileStatement(TokenReference where,
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
      CLoopContext	condContext = new CLoopContext(context, this);
      cond = cond.analyse(new CExpressionContext(condContext));
      condContext.close(getTokenReference());
      check(context,
	    cond.getType() == CStdType.Boolean,
	    KjcMessages.WHILE_COND_NOTBOOLEAN, cond.getType());

      if (!cond.isConstant()) {
	CBodyContext	neverContext = context.cloneContext();
	CLoopContext	bodyContext = new CLoopContext(context, this);

	body.analyse(bodyContext);
	bodyContext.close(getTokenReference());

	context.merge(neverContext);
      } else {
	// JLS 14.20 Unreachable Statements :
	// The contained statement [of a while statement] is reachable iff the while
	// statement is reachable and the condition expression is not a constant
	// expression whose value is false.
	check(context, cond.booleanValue(), KjcMessages.STATEMENT_UNREACHABLE);

	if (cond instanceof JAssignmentExpression) {
	  context.reportTrouble(new CWarning(getTokenReference(),
					     KjcMessages.ASSIGNMENT_IN_CONDITION));
	}

	CLoopContext	bodyContext;

	bodyContext = new CLoopContext(context, this);
	body.analyse(bodyContext);
	if (bodyContext.isBreakTarget()) {
	  bodyContext.adopt(bodyContext.getBreakContextSummary());
	}
	bodyContext.close(getTokenReference());
	context.setReachable(bodyContext.isBreakTarget());
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
    p.visitWhileStatement(this, cond, body);
  }
    
     /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return p.visitWhileStatement(this, cond, body);
  }


  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    setLineNumber(code);

    code.pushContext(this);

    if (cond.isConstant() && cond.booleanValue()) {
      // while (true)
      code.plantLabel(getContinueLabel());			//	body:
      body.genCode(code);					//		BODY CODE
      code.plantJumpInstruction(opc_goto, getContinueLabel());		//		GOTO body
    } else {
      CodeLabel		bodyLabel = new CodeLabel();

      code.plantJumpInstruction(opc_goto, getContinueLabel());		//		GOTO cont
      code.plantLabel(bodyLabel);				//	body:
      body.genCode(code);					//		BODY CODE
      code.plantLabel(getContinueLabel());			//	cont:
      cond.genBranch(true, code, bodyLabel);			//		EXPR CODE; IFNE body
    }
    code.plantLabel(getBreakLabel());				//	end:	...

    code.popContext(this);
  }

    /**
     * Set the condition of this.
     */
    public void setCondition(JExpression cond) {
	this.cond = cond;
    }

    /**
     * Sets the body of this.
     */
    public void setBody(JStatement body) {
	this.body = body;
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JExpression		cond;
  private JStatement		body;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JWhileStatement other = new at.dms.kjc.JWhileStatement();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JWhileStatement other) {
  super.deepCloneInto(other);
  other.cond = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.cond);
  other.body = (at.dms.kjc.JStatement)at.dms.kjc.AutoCloner.cloneToplevel(this.body);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
