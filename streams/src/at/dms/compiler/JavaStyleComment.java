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
 * $Id: JavaStyleComment.java,v 1.7 2003-08-29 19:25:33 thies Exp $
 */

package at.dms.compiler;

import java.io.*;

/**
 * A simple character constant
 */
public class JavaStyleComment implements Serializable, at.dms.kjc.DeepCloneable {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JavaStyleComment() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	text		the string representation of this comment
   * @param	!!! COMPLETE
   */
  public JavaStyleComment(String text, boolean isLineComment, boolean spaceBefore, boolean spaceAfter) {
    this.text = text;
    this.isLineComment = isLineComment;
    this.spaceBefore = spaceBefore;
    this.spaceAfter = spaceAfter;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   *
   */
  public String getText() {
    return text;
  }

  /**
   *
   */
  public boolean isLineComment() {
    return isLineComment;
  }

  /**
   *
   */
  public boolean hadSpaceBefore() {
    return spaceBefore;
  }

  /**
   *
   */
  public boolean hadSpaceAfter() {
    return spaceAfter;
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  protected	String		text;
    private /* final */ boolean		isLineComment; // removed "final" for cloner
    private /* final */ boolean		spaceBefore; // removed "final" for cloner
    private  /* final */ boolean		spaceAfter; // removed "final" for cloner

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.compiler.JavaStyleComment other = new at.dms.compiler.JavaStyleComment();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.compiler.JavaStyleComment other) {
  other.text = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.text, other);
  other.isLineComment = this.isLineComment;
  other.spaceBefore = this.spaceBefore;
  other.spaceAfter = this.spaceAfter;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
