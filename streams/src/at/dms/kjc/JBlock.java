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
 * $Id: JBlock.java,v 1.7 2001-10-12 10:23:42 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.List;

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

    // fill list with elements of <body>
    this.body = new LinkedList();
    for (int i=0; i<body.length; i++) {
	this.body.add(body[i]);
    }
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
    return body.size() == 0;
  }

    /**
     * Adds <statement> to this.
     */
    public void addStatement(JStatement statement) {
	body.add(statement);
    }

    /**
     * Adds <statement> to front of this.
     */
    public void addStatementFirst(JStatement statement) {
	body.addFirst(statement);
    }

    /**
     * Adds all statements in <lst> to this, at the specified position.
     */
    public void addAllStatements(int pos, List lst) {
	body.addAll(pos, lst);
    }

    /**
     * Adds all statements in <lst> to end of this.
     */
    public void addAllStatements(List lst) {
	body.addAll(lst);
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

    for (int i = 0; i < body.size(); i++) {
      if (! self.isReachable()) {
	throw new CLineError(((JStatement)body.get(i)).getTokenReference(), KjcMessages.STATEMENT_UNREACHABLE);
      }
      try {
	((JStatement)body.get(i)).analyse(self);
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
    p.visitBlockStatement(this, getStatements(), getComments());
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitBlockStatement(this, getStatements(), getComments());
  }

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    setLineNumber(code);

    for (int i = 0; i < body.size(); i++) {
      ((JStatement)body.get(i)).genCode(code);
    }
  }

 // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    /**
     * Returns array (NOT internal representation!!) of body of this.  
     */
    private JStatement[] getStatements() {
	return (JStatement[])body.toArray(new JStatement[0]);
    }

    /**
     * Returns list of statements in this.  
     */
    public List getStatementList() {
	return body;
    }

    /**
     * Returns iterator of statements in this.  
     */
    public ListIterator getStatementIterator() {
	return body.listIterator();
    }

    public void removeStatement(int i) {
	body.remove(i);
    }

    // <body> containts JStatements
  protected LinkedList 	body;
}

