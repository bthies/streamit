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
 * $Id: CommonToken.java,v 1.1 2001-08-30 16:32:39 thies Exp $
 */

package at.dms.compiler.tools.antlr.runtime;

public class CommonToken extends Token {
  // most tokens will want line and text information
  protected int line;
  protected String text = null;
  protected int col;

  public CommonToken() {}

  public CommonToken(int t, String txt) {
    type = t;
    setText(txt);
  }

  public CommonToken(String s)	{ text = s; }

  public int  getLine()		{ return line; }

  public String getText()		{ return text; }

  public void setLine(int l)		{ line = l; }

  public void setText(String s)	{ text = s; }

  public String toString() {
    return "[\""+getText()+"\",<"+type+">,line="+line+",col="+col+"]";
  }

  /**
   * Return token's start column
   */
  public int getColumn() { return col; }

  public void setColumn(int c) { col = c; }
}
