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
 * $Id: JForStatement.java,v 1.11 2003-08-21 09:44:20 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.JavaStyleComment;

/**
 * JLS 14.11: While Statement
 *
 * The while statement executes an expression and a statement repeatedly
 * until the value of the expression is false.
 */
public class JForStatement extends JLoopStatement {
    private boolean unrolled; //To not unroll the same loop several times
  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JForStatement() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	init		the init part
   * @param	cond		the cond part
   * @param	incr		the increment part
   * @param	body		the loop body.
   */
  public JForStatement(TokenReference where,
		       JStatement init,
		       JExpression cond,
		       JStatement incr,
		       JStatement body,
		       JavaStyleComment[] comments)
  {
    super(where, comments);

    this.init = init;
    this.cond = cond;
    this.incr = incr;
    this.body = body;
    unrolled=false;
  }

    public JStatement getInit() {
	return init;
    }

    public JStatement getIncrement() {
	return incr;
    }

    public JExpression getCondition() {
	return cond;
    }

    public JStatement getBody() {
	return body;
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
      CBlockContext bodyContext = new CBlockContext(context,
					     init instanceof JVariableDeclarationStatement ?
					     ((JVariableDeclarationStatement)init).getVars().length :
					     0);
      if (init != null) {
	init.analyse(bodyContext);
      }

      if (cond != null) {
	cond = cond.analyse(new CExpressionContext(bodyContext));
	check(bodyContext,
	      cond.getType() == CStdType.Boolean,
	      KjcMessages.FOR_COND_NOTBOOLEAN, cond.getType());
	if (cond.isConstant()) {
	  // JLS 14.20 Unreachable Statements :
	  // The contained statement [of a for statement] is reachable iff the
	  // for statement is reachable and the condition expression is not a constant
	  // expression whose value is false.
	  check(context, cond.booleanValue(), KjcMessages.STATEMENT_UNREACHABLE);

	  // for (A; true; B) equivalent to for (A; ; B)
	  cond = null;
	}
      }

      CBodyContext	neverContext = cond == null ? null : bodyContext.cloneContext();

      CLoopContext	loopContext = new CLoopContext(bodyContext, this);

      if (init instanceof JVariableDeclarationStatement) {
	((JVariableDeclarationStatement)init).setIsInFor();
      }
      body.analyse(loopContext);
      if (init instanceof JVariableDeclarationStatement) {
	((JVariableDeclarationStatement)init).unsetIsInFor();
      }

      if (neverContext == null && loopContext.isBreakTarget()) {
	loopContext.adopt(loopContext.getBreakContextSummary());
      }

      loopContext.close(getTokenReference());

      if (incr != null) {
	incr.analyse(bodyContext);
      }

      bodyContext.close(getTokenReference());

      if (neverContext != null) {
	context.merge(neverContext);
      } else if (!loopContext.isBreakTarget()) {
	context.setReachable(false);
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
    p.visitForStatement(this, init, cond, incr, body);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitForStatement(this, init, cond, incr, body);
  }

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    setLineNumber(code);

    CodeLabel		startLabel = new CodeLabel();
    CodeLabel		condLabel = new CodeLabel();

    code.pushContext(this);
    if (init != null) {
      init.genCode(code);			//		INIT
    }
    code.plantJumpInstruction(opc_goto, condLabel);	//		GOTO cond
    code.plantLabel(startLabel);		//	start:
    if (body != null) {
      body.genCode(code);			//		BODY
    }
    code.plantLabel(getContinueLabel());		//	incr:
    if (incr != null) {
      incr.genCode(code);			//		INCR
    }
    code.plantLabel(condLabel);			//	cond:
    if (cond != null) {
      cond.genBranch(true, code, startLabel);	//		COND IFNE start
    } else {
      code.plantJumpInstruction(opc_goto, startLabel);	//		GOTO start
    }
    code.plantLabel(getBreakLabel());			//	end:

    code.popContext(this);
  }

    /**
     * Sets the body of this.
     */
    public void setBody(JStatement body) {
	this.body = body;
    }

    /**
     * Sets the init of this.
     */
    public void setInit(JStatement init) {
	this.init = init;
    }

    /**
     * Sets the condition of this.
     */
    public void setCond(JExpression cond) {
	this.cond = cond;
    }

    /**
     * Sets the increment statement of this.
     */
    public void setIncr(JStatement incr) {
	this.incr = incr;
    }

    public boolean getUnrolled() {
	return unrolled;
    }

    public void setUnrolled(boolean unrolled) {
	this.unrolled = unrolled;
    }


  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JStatement		init;
  private JExpression		cond;
  private JStatement		incr;
  private JStatement		body;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JForStatement other = new at.dms.kjc.JForStatement();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JForStatement other) {
  super.deepCloneInto(other);
  other.unrolled = this.unrolled;
  other.init = (at.dms.kjc.JStatement)at.dms.kjc.AutoCloner.cloneToplevel(this.init, this);
  other.cond = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.cond, this);
  other.incr = (at.dms.kjc.JStatement)at.dms.kjc.AutoCloner.cloneToplevel(this.incr, this);
  other.body = (at.dms.kjc.JStatement)at.dms.kjc.AutoCloner.cloneToplevel(this.body, this);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
