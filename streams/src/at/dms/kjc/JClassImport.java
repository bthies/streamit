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
 * $Id: JClassImport.java,v 1.3 2003-05-16 21:06:38 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.CWarning;
import at.dms.compiler.Compiler;
import at.dms.compiler.TokenReference;
import at.dms.compiler.JavaStyleComment;

/**
 * JLS 7.5.1 Single-Type-Import Declaration.
 *
 * This class represents a single-type-import declaration
 * in the syntax tree.
 */
public class JClassImport extends JPhylum {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

  /**
   * Constructs a single-type-import declaration node in the syntax tree.
   *
   * @param	where		the line of this node in the source code
   * @param	name		the canonical name of the type
   * @param	comments	other comments in the source code
   */
  public JClassImport(TokenReference where,
		      String name,
		      JavaStyleComment[] comments)
  {
    super(where);

    this.name = name;
    this.comments = comments;

    this.used = false;

    int		index = name.lastIndexOf('/');

    this.ident = index == -1 ? name : name.substring(index + 1).intern();
  }

  // ----------------------------------------------------------------------
  // ACCESSORS & MUTATORS
  // ----------------------------------------------------------------------

  /**
   * Returns the fully qualified name of the imported type.
   */
  public String getQualifiedName() {
    return name;
  }

  /**
   * Returns the simple qualified name of the imported type.
   */
  public String getSimpleName() {
    return ident;
  }

  /**
   * States that specified class is used.
   */
  public void setUsed() {
    used = true;
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Analyses the statement (semantically).
   * @param	context		the analysis context
   * @exception	PositionedError	the analysis detected an error
   */
  public void analyse(Compiler compiler) {
    if (!used && getTokenReference() != TokenReference.NO_REF) {
      compiler.reportTrouble(new CWarning(getTokenReference(),
					  KjcMessages.UNUSED_CLASS_IMPORT,
					  name.replace('/', '.'),
					  null));
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
    if (comments != null) {
      p.visitComments(comments);
    }
    p.visitClassImport(name);
  }

     /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      if (comments != null) {
	  return p.visitComments(comments);
      }
      return p.visitClassImport(name);
  }


  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    private /* final */ String			name;  // removed final for cloner
    private /* final */ String			ident;  // removed final for cloner
  private /* final */ JavaStyleComment[]	comments;  // removed final for cloner
  private boolean			used;
}
