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
 * $Id: CharQueue.java,v 1.1 2001-08-30 16:32:39 thies Exp $
 */

package at.dms.compiler.tools.antlr.runtime;

/**
 * A circular buffer object used by CharBuffer
 */
public class CharQueue {
  // Physical circular buffer of characters
  protected char[] buffer;
  // buffer.length-1 for quick modulous
  protected int sizeLessOne;
  // physical index of front token
  protected int offset;
  // number of characters in the queue
  protected int nbrEntries;


  public CharQueue(int minSize) {
    // Find first power of 2 >= to requested size
    int size;
    for (size = 2; size < minSize; size *= 2) {}
    init(size);
  }
  /**
   * Add token to end of the queue
   * @param tok The token to add
   */
  public final void append(char tok) {
    if (nbrEntries == buffer.length) {
      expand();
    }
    buffer[(offset + nbrEntries) & sizeLessOne] = tok;
    nbrEntries++;
  }
  /**
   * Fetch a token from the queue by index
   * @param idx The index of the token to fetch, where zero is the token at the front of the queue
   */
  public final char elementAt(int idx) {
    return buffer[(offset + idx) & sizeLessOne];
  }
  /**
   * Expand the token buffer by doubling its capacity
   */
  private final void expand() {
    char[] newBuffer = new char[buffer.length * 2];
    // Copy the contents to the new buffer
    // Note that this will store the first logical item in the
    // first physical array element.
    for (int i = 0; i < buffer.length; i++) {
      newBuffer[i] = elementAt(i);
    }
    // Re-initialize with new contents, keep old nbrEntries
    buffer = newBuffer;
    sizeLessOne = buffer.length - 1;
    offset = 0;
  }
  /**
   * Initialize the queue.
   * @param size The initial size of the queue
   */
  private final void init(int size) {
    // Allocate buffer
    buffer = new char[size];
    // Other initialization
    sizeLessOne = size - 1;
    offset = 0;
    nbrEntries = 0;
  }
  /**
   * Remove char from front of queue
   */
  public final void removeFirst() {
    offset = (offset+1) & sizeLessOne;
    nbrEntries--;
  }
}
