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
 * $Id: NoViableAltException.java,v 1.1 2001-08-30 16:32:39 thies Exp $
 */

package at.dms.compiler.tools.antlr.runtime;

public class NoViableAltException extends RecognitionException {

  public Token token;

  public NoViableAltException(Token t, String fileName) {
    super("NoViableAlt");
    token = t;
    line = t.getLine();
    column = t.getColumn();
    this.fileName = fileName;
  }

  /**
   * Returns a clean error message (no line number/column information)
   */
  public String getMessage() {
    return "unexpected token: "+token.getText();
  }

  /**
   * Returns a string representation of this exception.
   */
  public String toString() {
    return FileLineFormatter.getFormatter().getFormatString(fileName,line)+getMessage();
  }
}
