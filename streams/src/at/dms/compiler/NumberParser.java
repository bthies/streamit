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
 * $Id: NumberParser.java,v 1.1 2001-08-30 16:32:31 thies Exp $
 */

package at.dms.compiler;

/**
 * This class parse Strings to convert them to ordinal literal
 */
public class NumberParser {

  // ----------------------------------------------------------------------
  // PRIVATE METHODS
  // ----------------------------------------------------------------------

  public static long decodeLong(String text) {
    // Strip off negative sign, if any
    boolean	negative;

    if (text.startsWith("-")) {
      negative = true;
      text = text.substring(1);
    } else {
      negative = false;
    }

    // Strip off base indicator, if any
    if (text.startsWith("0x") || text.startsWith("0X")) {
      return decodeHexLong(negative, text.substring(2));
    } else if (text.startsWith("0")) {
      // do not strip leading 0 to allow parsing of 0
      return decodeOctLong(negative, text);
    } else {
      return decodeDecLong(negative, text);
    }
  }

  public static int decodeInt(String text) {
    // Strip off negative sign, if any
    boolean	negative;

    if (text.startsWith("-")) {
      negative = true;
      text = text.substring(1);
    } else {
      negative = false;
    }

    // Strip off base indicator, if any
    if (text.startsWith("0x") || text.startsWith("0X")) {
      return decodeHexInt(negative, text.substring(2));
    } else if (text.startsWith("0")) {
      // do not strip leading 0 to allow parsing of 0
      return decodeOctInt(negative, text);
    } else if (text.equals("2147483648") && negative) {
      return Integer.MIN_VALUE;
    } else {
      if (negative) {
	return - Integer.decode(text).intValue();
      } else {
	return Integer.decode(text).intValue();
      }
    }
  }

  public static long decodeDecLong(boolean negative, String text) {
    long		result = 0;

    for (int pos = 0; pos < text.length(); pos++) {
      int	digit = Character.digit(text.charAt(pos), 10);

      if (digit == -1) {
	throw new NumberFormatException("invalid digit");
      }

      if (result < Long.MIN_VALUE / 10) {
	throw new NumberFormatException("overflow");
      }

      result *= 10;

      if (result < Long.MIN_VALUE + digit) {
	throw new NumberFormatException("overflow");
      }

      result -= digit;
    }

    if (result == Long.MIN_VALUE && ! negative) {
      throw new NumberFormatException("overflow");
    }

    if (negative) {
      return result;
    } else {
      return - result;
    }
  }

  public static long decodeHexLong(boolean negative, String text) {
    if (text.length() == 0) {
      throw new NumberFormatException("empty string");
    }

    long		result = 0;
    long		length = 0;

    for (int pos = 0; pos < text.length(); pos++) {
      int	digit = Character.digit(text.charAt(pos), 16);

      if (digit == -1) {
	throw new NumberFormatException("invalid digit");
      }

      // ignore leading zeroes
      if (digit != 0 || length != 0) {
	if (length == 16) {
	  throw new NumberFormatException("overflow");
	}

	result = (result << 4) | digit;
	length += 1;
      }
    }

    if (negative) {
      return - result;
    } else {
      return result;
    }
  }

  public static long decodeOctLong(boolean negative, String text) {
    long		result = 0;
    long		length = 0;

    for (int pos = 0; pos < text.length(); pos++) {
      int	digit = Character.digit(text.charAt(pos), 8);

      if (digit == -1) {
	throw new NumberFormatException("invalid digit");
      }

      // ignore leading zeroes
      if (digit != 0 || length != 0) {
	if (length == 22) {
	  throw new NumberFormatException("overflow");
	}
	if (length == 21 && (result & 0600000000000000000000L) != 0L) {
	  throw new NumberFormatException("overflow");
	}

	result = (result << 3) | digit;
	length += 1;
      }
    }

    if (negative) {
      return - result;
    } else {
      return result;
    }
  }

  public static int decodeHexInt(boolean negative, String text) {
    if (text.length() == 0) {
      throw new NumberFormatException("empty string");
    }

    int		result = 0;
    int		length = 0;

    for (int pos = 0; pos < text.length(); pos++) {
      int	digit = Character.digit(text.charAt(pos), 16);

      if (digit == -1) {
	throw new NumberFormatException("invalid digit");
      }

      // ignore leading zeroes
      if (digit != 0 || length != 0) {
	if (length == 8) {
	  throw new NumberFormatException("overflow");
	}

	result = (result << 4) | digit;
	length += 1;
      }
    }

    if (negative) {
      return - result;
    } else {
      return result;
    }
  }

  public static int decodeOctInt(boolean negative, String text) {
    int		result = 0;
    int		length = 0;

    for (int pos = 0; pos < text.length(); pos++) {
      int	digit = Character.digit(text.charAt(pos), 8);

      if (digit == -1) {
	throw new NumberFormatException("invalid digit");
      }

      // ignore leading zeroes
      if (digit != 0 || length != 0) {
	if (length == 11) {
	  throw new NumberFormatException("overflow");
	}
	if (length == 10 && (result & 04000000000) != 0) {
	  throw new NumberFormatException("overflow");
	}

	result = (result << 3) | digit;
	length += 1;
      }
    }

    if (negative) {
      return - result;
    } else {
      return result;
    }
  }
}
