/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: UnicodeReader.java,v 1.1 2002-12-11 20:14:58 karczma Exp $
 */

package at.dms.util;

import java.io.EOFException;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * A character-stream reader that transforms unicode escape sequences
 * into characters according to the Java Language Specification - Second Edition.
 *
 * See JLS 3.2 : Lexical Translations, Step 1
 * See JLS 3.3 : Unicode Escapes
 *
 * NOTE: we use a lookahead character instead of a pushback reader because
 * for example the sequence "\\u..." would be read as "\" and "\ u . . ."
 */
public class UnicodeReader extends FilterReader {

  /**
   * Creates a new unicode reader.
   *
   * @param	in		the reader from which characters will be read
   */
  public UnicodeReader(Reader in) {
    super(in);
    lookahead = -2;
  }

  /**
   * Reads a single character.
   * @return	the character read, or -1 if the end of the stream has been reached
   * @throws	IOException	if an I/O error occurs
   */
  public int read() throws IOException {
    if (lookahead != -2) {
      int		c;

      c = lookahead;
      lookahead = -2;
      return c;
    } else {
      int		c;

      c = super.read();
      if (c != '\\') {
	return c;
      } else {
	c = super.read();
	if (c != 'u') {
	  lookahead = c;
	  return '\\';
	} else {
	  do {
	    c = super.read();
	  } while (c == 'u');

	  int	value;

	  value = decodeHexDigit(c);
	  value = (value << 4) + decodeHexDigit(super.read());
	  value = (value << 4) + decodeHexDigit(super.read());
	  value = (value << 4) + decodeHexDigit(super.read());

	  return value;
	}
      }
    }
  }

  /**
   * Returns the numeric value of the character c interpreted as hexadecimal digit.
   * @param	c		the character to be converted
   * @return	the numeric value represented by the character in radix 16
   * @throws	IOException	if the character represents EOF or is not a hexadecimal digit
   */
  private static int decodeHexDigit(int c) throws IOException {
    int		result;

    if (c == -1) {
      throw new EOFException("end of file reached while scanning unicode escape sequence");
    }
    result = Character.digit((char)c, 16);
    if (result == -1) {
      throw new EOFException("invalid character " + (char)c + " in unicode escape sequence");
    }
    return result;
  }

  /**
   * Reads characters into a portion of an array.
   * @param	buf		destination buffer
   * @param	off		offset at which to start writing characters
   * @param	len		maximum number of characters to read
   * @return	the number of characters read, or -1 if the end of the stream has been reached
   * @throws	IOException	if an I/O error occurs
   */
  public int read(char[] buf, int off, int len)
    throws IOException
  {
    int		act = 0;	// # of characters actually read
    int		c;

    while (act < len && (c = read()) != -1) {
      buf[off + act] = (char)c;
      act += 1;
    }

    return act == 0 ? -1 : act;
  }

  /**
   * Tell whether this stream supports the mark() operation.
   * @return	false: this stream does not support mark()
   */
  public boolean markSupported() {
    return false;
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private int		lookahead = -2;		// lookahead character
}
