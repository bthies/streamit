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
 * $Id: MismatchedCharException.java,v 1.1 2001-08-30 16:32:39 thies Exp $
 */

package at.dms.compiler.tools.antlr.runtime;

public class MismatchedCharException extends RecognitionException {
  // Types of chars
  public static final int CHAR = 1;
  public static final int NOT_CHAR = 2;
  public static final int RANGE = 3;
  public static final int NOT_RANGE = 4;
  public static final int SET = 5;
  public static final int NOT_SET = 6;

  // One of the above
  public int mismatchType;

  // what was found on the input stream
  public char foundChar;

  // For CHAR/NOT_CHAR and RANGE/NOT_RANGE
  public int expecting;

  // For RANGE/NOT_RANGE (expecting is lower bound of range)
  public int upper;

  // For SET/NOT_SET
  public BitSet set;

  // who knows...they may want to ask scanner questions
  public CharScanner scanner;

  /**
   * MismatchedCharException constructor comment.
   */
  public MismatchedCharException() {
    super("Mismatched char");
  }

  // Expected range / not range
  public MismatchedCharException(char c, char lower, char upper_, boolean matchNot, CharScanner scanner) {
    super("Mismatched char");
    foundChar = c;
    expecting = lower;
    upper = upper_;
    // get instantaneous values of file/line/column
    this.line = scanner.getLine();
    this.fileName = scanner.getFilename();
    this.column = scanner.getColumn();
    this.scanner = scanner;
    mismatchType = matchNot ? NOT_RANGE : RANGE;
  }

  // Expected token / not token
  public MismatchedCharException(char c, char expecting_, boolean matchNot, CharScanner scanner) {
    super("Mismatched char");
    foundChar = c;
    expecting = expecting_;
    // get instantaneous values of file/line/column
    this.line = scanner.getLine();
    this.fileName = scanner.getFilename();
    this.column = scanner.getColumn();
    this.scanner = scanner;
    mismatchType = matchNot ? NOT_CHAR : CHAR;
  }

  // Expected BitSet / not BitSet
  public MismatchedCharException(char c, BitSet set_, boolean matchNot, CharScanner scanner) {
    super("Mismatched char");
    foundChar = c;
    set = set_;
    // get instantaneous values of file/line/column
    this.line = scanner.getLine();
    this.fileName = scanner.getFilename();
    this.column = scanner.getColumn();
    this.scanner = scanner;
    mismatchType = matchNot ? NOT_SET : SET;
  }

  /**
   * MismatchedCharException constructor comment.
   * @param s java.lang.String
   */
  public MismatchedCharException(String s, int line) {
    super(s);
  }

  /**
   * Returns the error message that happened on the line/col given.
   * Copied from toString().
   */
  public String getMessage() {
    StringBuffer sb = new StringBuffer();

    switch (mismatchType) {
    case CHAR :
      sb.append("expecting '" + (char)expecting + "', found '" + (char)foundChar + "'");
      break;
    case NOT_CHAR :
      sb.append("expecting anything but '" + (char)expecting + "'; got it anyway");
      break;
    case RANGE :
      sb.append("expecting token in range: '" + (char)expecting + "'..'" + (char)upper + "', found '" + (char)foundChar + "'");
      break;
    case NOT_RANGE :
      sb.append("expecting token NOT in range: " + (char)expecting + "'..'" + (char)upper + "', found '" + (char)foundChar + "'");
      break;
    case SET :
    case NOT_SET :
      sb.append("expecting " + (mismatchType == NOT_SET ? "NOT " : "") + "one of (");
      int[] elems = set.toArray();
      for (int i = 0; i < elems.length; i++) {
	sb.append(" '");
	sb.append((char)elems[i]);
	sb.append("'");
      }
      sb.append("), found '" + (char)foundChar + "'");
      break;
    default :
      sb.append(super.getMessage());
      break;
    }

    return sb.toString();
  }
}
