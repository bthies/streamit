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
 * $Id: RecognitionException.java,v 1.1 2001-08-30 16:32:39 thies Exp $
 */

package at.dms.compiler.tools.antlr.runtime;

public class RecognitionException extends ParserException {
  public String fileName;		// not used by treeparsers
  public int line;			// not used by treeparsers
  public int column;			// not used by treeparsers

  public RecognitionException() {
    super("parsing error");
  }

  /**
   * RecognitionException constructor comment.
   * @param s java.lang.String
   */
  public RecognitionException(String s) {
    super(s);
  }

  /**
   * RecognitionException constructor comment.
   * @param s java.lang.String
   */
  public RecognitionException(String s, String fileName, int line) {
    super(s);
    this.fileName = fileName;
    this.line = line;
  }

  public int getColumn() { return column; }

  /**
   * @deprecated As of ANTLR 2.7.0
   */
  public String getErrorMessage () { return getMessage(); }

  public String getFilename() {
    return fileName;
  }

  public int getLine() { return line; }

  public String toString() {
    return FileLineFormatter.getFormatter().
      getFormatString(fileName,line)+getMessage();
  }
}
