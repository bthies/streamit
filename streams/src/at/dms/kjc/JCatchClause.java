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
 * $Id: JCatchClause.java,v 1.5 2003-08-21 09:44:20 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.CWarning;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * This class represents a parameter declaration in the syntax tree
 */
public class JCatchClause extends JPhylum {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JCatchClause() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	exception	the exception caught
   * @param	body		the body of the exception handler
   */
  public JCatchClause(TokenReference where,
		      JFormalParameter exception,
		      JBlock body)
  {
    super(where);

    this.exception = exception;
    this.body = body;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * getType
   * @return	the type of exception catched by this clause
   */
  public CClassType getType() {
    return (CClassType)exception.getType();
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Analyses the node (semantically).
   * @param	context		the analysis context
   * @exception	PositionedError	the analysis detected an error
   */
  public void analyse(CBodyContext context) throws PositionedError {
    CBlockContext	block = new CBlockContext(context, 1);

    block.setReachable(true);
    exception.analyse(block);
    //!!! Throwable !!!
    body.analyse(block);

    block.close(getTokenReference());

    if (body.isEmpty()) {
      context.reportTrouble(new CWarning(getTokenReference(),
					 KjcMessages.EMPTY_CATCH_BLOCK));
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
    p.visitCatchClause(this, exception, body);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitCatchClause(this, exception, body);
  }

  /**
   * Generates bytecode for the exception handler.
   *
   * @param	code		the code sequence
   * @param	start		the beginning of the checked area (inclusive)
   * @param	end		the end of the checked area (exclusive !)
   */
  public void genCode(CodeSequence code, int start, int end) {
    setLineNumber(code);

    int		catchPC;

    catchPC = code.getPC();
    exception.genStore(code);
    body.genCode(code);

    code.addExceptionHandler(start,
			     end,
			     catchPC,
			     exception.getType().getCClass().getQualifiedName());
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JFormalParameter	exception;
  private JBlock		body;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JCatchClause other = new at.dms.kjc.JCatchClause();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JCatchClause other) {
  super.deepCloneInto(other);
  other.exception = (at.dms.kjc.JFormalParameter)at.dms.kjc.AutoCloner.cloneToplevel(this.exception, this);
  other.body = (at.dms.kjc.JBlock)at.dms.kjc.AutoCloner.cloneToplevel(this.body, this);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
