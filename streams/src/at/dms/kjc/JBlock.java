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
 * $Id: JBlock.java,v 1.1 2001-08-30 16:32:51 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * JLS 14.2: Block
 *
 * TA block is a sequence of statements and local variable declaration
 * statements within braces.
 */
public class JBlock extends JStatement {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	body		the statements contained in the block
   * @param	comment		other comments in the source code
   */
  public JBlock(TokenReference where,
		JStatement[] body,
		JavaStyleComment[] comments)
  {
    super(where, comments);

    this.body = body;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Tests whether the block is empty.
   *
   * @return	true iff the block is empty.
   */
  public boolean isEmpty() {
    return body.length == 0;
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
    CBlockContext	self = new CBlockContext(context);

    for (int i = 0; i < body.length; i++) {
      if (! self.isReachable()) {
	throw new CLineError(body[i].getTokenReference(), KjcMessages.STATEMENT_UNREACHABLE);
      }
      try {
	body[i].analyse(self);
      } catch (CLineError e) {
	self.reportTrouble(e);
      }
    }

    self.close(getTokenReference());
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    p.visitBlockStatement(this, body, getComments());
  }

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    setLineNumber(code);

    for (int i = 0; i < body.length; i++) {
      body[i].genCode(code);
    }
  }

 // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  protected final JStatement[]		body;
}
