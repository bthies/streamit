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
 * $Id: MismatchedTokenException.java,v 1.2 2002-12-11 23:56:11 karczma Exp $
 */

package at.dms.compiler.antlr.runtime;

public class MismatchedTokenException extends RecognitionException {
  // Token names array for formatting
  String[] tokenNames;
  // The token that was encountered
  public Token token;

  String tokenText=null; // taken from node or token object

  // Types of tokens
  public static final int TOKEN = 1;
  public static final int NOT_TOKEN = 2;
  public static final int RANGE = 3;
  public static final int NOT_RANGE = 4;
  public static final int SET = 5;
  public static final int NOT_SET = 6;
  // One of the above
  public int mismatchType;

  // For TOKEN/NOT_TOKEN and RANGE/NOT_RANGE
  public int expecting;

  // For RANGE/NOT_RANGE (expecting is lower bound of range)
  public int upper;

  // For SET/NOT_SET
  public BitSet set;

  /**
   * Looking for AST wildcard, didn't find it
   */
  public MismatchedTokenException() {
    super("Mismatched Token: expecting any AST node");
  }

  // Expected range / not range
  public MismatchedTokenException(String[] tokenNames_, Token token_, int lower, int upper_, boolean matchNot, String fileName) {
    super("Mismatched Token");
    tokenNames = tokenNames_;
    token = token_;
    line = token.getLine();
    column = token.getColumn();
    tokenText = token.getText();
    expecting = lower;
    upper = upper_;
    this.fileName = fileName;
    mismatchType = matchNot ? NOT_RANGE : RANGE;
  }
  // Expected token / not token
  public MismatchedTokenException(String[] tokenNames_, Token token_, int expecting_, boolean matchNot, String fileName) {
    super("Mismatched Token");
    tokenNames = tokenNames_;
    token = token_;
    line = token.getLine();
    column = token.getColumn();
    tokenText = token.getText();
    expecting = expecting_;
    this.fileName = fileName;
    mismatchType = matchNot ? NOT_TOKEN : TOKEN;
  }
  // Expected BitSet / not BitSet
  public MismatchedTokenException(String[] tokenNames_, Token token_, BitSet set_, boolean matchNot, String fileName) {
    super("Mismatched Token");
    tokenNames = tokenNames_;
    token = token_;
    line = token.getLine();
    column = token.getColumn();
    tokenText = token.getText();
    set = set_;
    this.fileName = fileName;
    mismatchType = matchNot ? NOT_SET : SET;
  }

  /**
   * Returns the error message that happened on the line/col given.
   * Copied from toString().
   */
  public String getMessage() {
    StringBuffer sb = new StringBuffer();

    switch (mismatchType) {
    case TOKEN :
      sb.append("expecting " + tokenName(expecting) + ", found '" + tokenText + "'");
      break;
    case NOT_TOKEN :
      sb.append("expecting anything but " + tokenName(expecting) + "; got it anyway");
      break;
    case RANGE :
      sb.append("expecting token in range: " + tokenName(expecting) + ".." + tokenName(upper) + ", found '" + tokenText + "'");
      break;
    case NOT_RANGE :
      sb.append("expecting token NOT in range: " + tokenName(expecting) + ".." + tokenName(upper) + ", found '" + tokenText + "'");
      break;
    case SET :
    case NOT_SET :
      sb.append("expecting " + (mismatchType == NOT_SET ? "NOT " : "") + "one of (");
      int[] elems = set.toArray();
      for (int i = 0; i < elems.length; i++) {
	sb.append(" ");
	sb.append(tokenName(elems[i]));
      }
      sb.append("), found '" + tokenText + "'");
      break;
    default :
      sb.append(super.getMessage());
      break;
    }

    return sb.toString();
  }

  private String tokenName(int tokenType) {
    if (tokenType == Token.INVALID_TYPE) {
      return "<Set of tokens>";
    } else if (tokenType < 0 || tokenType >= tokenNames.length) {
      return "<" + String.valueOf(tokenType) + ">";
    } else {
      return tokenNames[tokenType];
    }
  }

  /**
   * @return a string representation of this exception.
   */
  public String toString() {
    return FileLineFormatter.getFormatter().getFormatString(fileName,line)+getMessage();
  }
}
