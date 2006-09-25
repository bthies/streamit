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
 * $Id: CharArrayCache.java,v 1.3 2006-09-25 13:54:51 dimock Exp $
 */

package at.dms.util;

import java.util.Stack;

/**
 * This class implements a cache of char arrays
 */
public class CharArrayCache {

    /**
     * Returns a char array.
     */
    public static char[] request() {
        if (stack.empty()) {
            return new char[ARRAY_SIZE];
        } else {
            return stack.pop();
        }
    }

    /**
     * Releases a char array.
     */
    public static void release(char[] array) {
        stack.push(array);
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    private static final int    ARRAY_SIZE = 100000;

    private static Stack<char[]>        stack = new Stack<char[]>();
}
