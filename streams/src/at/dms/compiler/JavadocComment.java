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
 * $Id: JavadocComment.java,v 1.3 2003-05-28 05:58:39 thies Exp $
 */

package at.dms.compiler;

/**
 * A simple character constant
 */
public class JavadocComment extends JavaStyleComment {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JavadocComment() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	text		the string representation of this comment
   */
  public JavadocComment(String text, boolean spaceBefore, boolean spaceAfter) {
    super(text, false, spaceBefore, spaceAfter);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Return if this javadoc comment contains a deprecated clause
   */
  public boolean isDeprecated() {
    return text.indexOf("@deprecated") >= 0;
  }

  /**
   *
   */
  public String getParams() {
    return text;
  }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.compiler.JavadocComment other = new at.dms.compiler.JavadocComment();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.compiler.JavadocComment other) {
  super.deepCloneInto(other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
