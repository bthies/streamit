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
 * $Id: JBlock.java,v 1.17 2003-08-29 19:25:36 thies Exp $
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

    /**
     * Construct a node in the parsing tree
     * @param	where		the line of this node in the source code
     * @param	body		a list of statements contained in the block
     * @param	comment		other comments in the source code
     */
    public JBlock(TokenReference where,
		  List body,
		  JavaStyleComment[] comments)
  {
    super(where, comments);
    // make a copy of <body>
    this.body = new LinkedList(body);
  }

    /**
     * Construct a new JBlock with no statements inside.
     */
    public JBlock() {
	this(null, new LinkedList(), null);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

    /**
     * Return a shallow copy of this block (don't copy any contained
     * statements; just copy list of statements.)
     */
    public JBlock copy() {
	return new JBlock(null, new LinkedList(body), getComments());
    }

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
     * Adds statement to this, at the specified position.
     */
    public void addStatement(int pos, JStatement statement) {
	body.add(pos, statement);
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
    p.visitBlockStatement(this, getComments());
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitBlockStatement(this, getComments());
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
     * Returns INTERNAL list of statements in this.  
     */
    public List getStatements() {
	return body;
    }

    /**
     * Returns i'th statement.
     */
    public JStatement getStatement(int i) {
	return (JStatement)body.get(i);
    }

    /**
     * Returns array of statements in this.  
     */
    public JStatement[] getStatementArray() {
	return (JStatement[])body.toArray(new JStatement[0]);
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

    public int size() {
	return body.size();
    }

    public void setStatement(int i, JStatement statement) {
	body.set(i, statement);
    }

    // <body> containts JStatements
  protected LinkedList 	body;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JBlock other = new at.dms.kjc.JBlock();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JBlock other) {
  super.deepCloneInto(other);
  other.body = (java.util.LinkedList)at.dms.kjc.AutoCloner.cloneToplevel(this.body, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

