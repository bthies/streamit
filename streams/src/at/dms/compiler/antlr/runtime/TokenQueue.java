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
 * $Id: TokenQueue.java,v 1.2 2002-12-11 23:56:11 karczma Exp $
 */

package at.dms.compiler.antlr.runtime;

/**
 * A private circular buffer object used by the token buffer
 */
class TokenQueue {
  // Physical circular buffer of tokens
  private Token[] buffer;
  // buffer.length-1 for quick modulous
  private int sizeLessOne;
  // physical index of front token
  private int offset;
  // number of tokens in the queue
  protected int nbrEntries;


  public TokenQueue(int minSize) {
    // Find first power of 2 >= to requested size
    int size;
    for (size = 2; size < minSize; size *= 2) {}
    init(size);
  }
  /**
   * Add token to end of the queue
   * @param tok The token to add
   */
  public final void append(Token tok) {
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
  public final Token elementAt(int idx) {
    return buffer[(offset + idx) & sizeLessOne];
  }
  /**
   * Expand the token buffer by doubling its capacity
   */
  private final void expand() {
    Token[] newBuffer = new Token[buffer.length * 2];
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
    buffer = new Token[size];
    // Other initialization
    sizeLessOne = size - 1;
    offset = 0;
    nbrEntries = 0;
  }
  /**
   * Remove token from front of queue
   */
  public final void removeFirst() {
    offset = (offset+1) & sizeLessOne;
    nbrEntries--;
  }
}
