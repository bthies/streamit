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
 * $Id: TabbedPrintWriter.java,v 1.3 2003-05-28 05:58:39 thies Exp $
 */

package at.dms.compiler;

import java.io.PrintWriter;
import java.io.Writer;

/**
 * This class implements a tabbed print writer
 */
public class TabbedPrintWriter implements at.dms.kjc.DeepCloneable {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    private TabbedPrintWriter() {} // for cloner only

  /**
   * construct a pretty printer object for java code
   * @param	fileName		the file into the code is generated
   */
  public TabbedPrintWriter(Writer writer) {
    p =  new PrintWriter(writer);

    this.pos = 0;
  }

  /**
   * Close the stream at the end
   */
  public void close() {
    p.close();
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  public int getPos() {
    return pos;
  }

  /**
   * Set pos
   */
  public void setPos(int pos) {
    this.pos = pos;
  }

  /**
   * Increment tab
   */
  public void add(int pos) {
    this.pos += pos;
  }

  /**
   * Decrement tab
   */
  public void sub(int pos) {
    this.pos += pos;
  }

  /**
   * Print a new line
   */
  public void println() {
    p.println();
    column = 0;
    line++;
  }

  /**
   * Print a string
   */
  public void print(String s) {
    /*if (Math.max(column, pos) + s.length() > 80 && s.length() > 2) {
      println();
    }*/
    checkPos();
    p.print(s);
    column += s.length();
  }

  // ----------------------------------------------------------------------
  // PRIVATE METHODS
  // ----------------------------------------------------------------------

  private void checkPos() {
    if (column < pos) {
      p.print(space(pos - column));
      column = Math.max(column, pos);
    }
  }

  private String space(int count) {
    if (count <= 0) {
      count = 1;
    }
    return spaceIn(count);
  }

  private String spaceIn(int count) {
    return new String(new char[count]).replace((char)0, ' ');
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private PrintWriter			p;

  protected int				pos;
  protected int				line;
  protected int				column;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.compiler.TabbedPrintWriter other = new at.dms.compiler.TabbedPrintWriter();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.compiler.TabbedPrintWriter other) {
  other.p = (java.io.PrintWriter)at.dms.kjc.AutoCloner.cloneToplevel(this.p);
  other.pos = this.pos;
  other.line = this.line;
  other.column = this.column;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
