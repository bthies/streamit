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
 * $Id: Keywords.java,v 1.1 2001-08-30 16:32:41 thies Exp $
 */

package at.dms.compiler.tools.jperf;

import java.io.PrintWriter;

/**
 * This class contains the keywords to hash
 */

public class Keywords {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Creates a Keywords object
   *
   * @param	keywords		the array of keywords.
   */
  public Keywords(String[] keywords) {
    this.keywords = keywords;

    int		minWordLength = Integer.MAX_VALUE;
    int		maxWordLength = 0;

    char	minCharValue = '~';	// '~' is the highest possible value
    char	maxCharValue = ' ';	// SPACE is the lowest possible value

    // Analyses the keywords.
    for (int i = 0; i < keywords.length; i++) {
      // how long are the longest and shortest key ?
      if (keywords[i].length() > maxWordLength) {
	maxWordLength = keywords[i].length();
      }
      if (keywords[i].length() < minWordLength) {
	minWordLength = keywords[i].length();
      }

      // what are the smallest and largest ASCII value in all keys ?
      for (int j = 0; j < keywords[i].length(); j++) {
	char		c = keywords[i].charAt(j);

	if (c < minCharValue) {
	  minCharValue = c;
	}
	if (c > maxCharValue) {
	  maxCharValue = c;
	}
      }
    }

    this.minWordLength = minWordLength;
    this.maxWordLength = maxWordLength;

    this.minCharValue = minCharValue;
    this.maxCharValue = maxCharValue;
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  /**
   * Returns the number of keywords.
   */
  public final int size() {
    return keywords.length;
  }

  /**
   * Returns the keyword at specified index. Keeps encapsulation.
   */
  public final String elementAt(int index) {
    return keywords[index];
  }

  /**
   * Returns the length of the longest keyword.
   */
  public final int getMaxWordLength() {
    return maxWordLength;
  }

  /**
   * Returns the smallest ASCII value in all keys.
   */
  public final char getMinCharValue() {
    return minCharValue;
  }

  /**
   * Returns the largest ASCII value in all keys.
   */
  public final char getMaxCharValue() {
    return maxCharValue;
  }

  // --------------------------------------------------------------------
  // CODE GENERATION
  // --------------------------------------------------------------------

  /**
   * Prints keyword related code to the output file.
   *
   * @param	out		the output stream
   */
  public void genCode(PrintWriter out) {
    // generate constants
    if (minCharValue == '\'') {
      out.println("  private static final int MIN_CHAR_VAL = '\\'';");
    } else {
      out.println("  private static final int MIN_CHAR_VAL = '" + minCharValue + "';");
    }
    if (maxCharValue == '\'') {
      out.println("  private static final int MAX_CHAR_VAL = '\\'';");
    } else {
      out.println("  private static final int MAX_CHAR_VAL = '" + maxCharValue + "';");
    }
    out.println("  private static final int MIN_WORD_LENG = " + minWordLength + ";");
    out.println("  private static final int MAX_WORD_LENG = " + maxWordLength + ";");
    out.println("  private static final int TOTAL_KEYWORDS = " + keywords.length + ";");

    // generate keyword table
    out.println("  private static final char[][] keywords = new char[][] {");
    for (int i = 0; i < keywords.length; i++) {
      if (i != 0) {
	out.println(",");
      }
      out.print("    \"" + keywords[i] + "\".toCharArray()");
    }
    out.println();
    out.println("  };");
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private final String[]	keywords;

  private final int		minWordLength;
  private final int		maxWordLength;
  private final char		minCharValue;
  private final char		maxCharValue;
}
