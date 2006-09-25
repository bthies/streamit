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
 * $Id: SimpleStringBuffer.java,v 1.3 2006-09-25 13:54:51 dimock Exp $
 */

package at.dms.util;

import java.util.Stack;

/**
 * A class to handle a sequence of characters.
 */
public final class SimpleStringBuffer {
    public void reset() {
        pos = 0;
    }

    /**
     * Returns a string representation of the data in this buffer.
     */
    public String toString() {
        return String.valueOf(buf, 0, pos);
    }

    /**
     * Appends the specified string to this buffer.
     * @param   s   the string to append
     */
    public void append(String s) {
        int     length = s.length();
        try {
            s.getChars(0, length, buf, pos);
            pos += length;
        } catch (Exception e) {
            grow(length + 16);
            append(s);
        }
    }

    /**
     * Appends the specified character to this buffer.
     * @param   c   the character to append
     */
    public void append(char c) {
        try {
            buf[pos] = c;
            pos++;
        } catch (Exception e) {
            grow(16);
            buf[pos++] = c;
        }
    }

    private void grow(int size) {
        char[]  newValue = new char[buf.length + size];

        System.arraycopy(buf, 0, newValue, 0, buf.length);
        buf = newValue;
    }


    // ----------------------------------------------------------------------
    // OBJECT POOLING
    // ----------------------------------------------------------------------

    // TODO : use ENV_USE_CACHE

    /**
     * Returns a new simple string buffer from the buffer pool.
     */
    public static SimpleStringBuffer request() {
        if (stack.empty()) {
            return new SimpleStringBuffer();
        } else {
            SimpleStringBuffer  buffer;

            buffer = stack.pop();
            buffer.reset();
            return buffer;
        }
    }

    /**
     * Returns an unused simple string buffer to the pool.
     */
    public static void release(SimpleStringBuffer buffer) {
        if (buffer != null) {
            stack.push(buffer);
        }
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    private static final int    BUFFER_SIZE = 100;

    private char[]      buf = new char[BUFFER_SIZE];
    private int         pos;

    /**
     * The stack of available buffers.
     */
    private static final Stack<SimpleStringBuffer>  stack = new Stack<SimpleStringBuffer>();
}
