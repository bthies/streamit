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
 * $Id: ANTLRStringBuffer.java,v 1.1 2001-08-30 16:32:39 thies Exp $
 */

package at.dms.compiler.tools.antlr.runtime;

// Implementation of a StringBuffer-like object that does not have the
// unfortunate side-effect of creating Strings with very large buffers.

public class ANTLRStringBuffer {
  protected char[] buffer = new char[8];
  protected int length = 0;		// length and also where to store next char


  public ANTLRStringBuffer() {}
  public final void append(char c) {
    // This would normally be  an "ensureCapacity" method, but inlined
    // here for speed.
    if (length >= buffer.length) {
      // Compute a new length that is at least double old length
      int newSize = buffer.length;
      while (length >= newSize) {
	newSize *= 2;
      }
      // Allocate new array and copy buffer
      char[] newBuffer = new char[newSize];
      for (int i = 0; i < length; i++) {
	newBuffer[i] = buffer[i];
      }
      buffer = newBuffer;
    }
    buffer[length] = c;
    length++;
  }
  public final void append(String s) {
    for (int i = 0; i < s.length(); i++) {
      append(s.charAt(i));
    }
  }
  public final char charAt(int index) { return buffer[index]; }
  final public char[] getBuffer() { return buffer; }
  public final int length() { return length; }
  public final void setCharAt(int index, char  ch) { buffer[index] = ch; }
  public final void setLength(int newLength) {
    if (newLength < length) {
      length = newLength;
    } else {
      while (newLength > length) {
	append('\0');
      }
    }
  }
  public final String toString() {
    return new String(buffer, 0, length);
  }
}
