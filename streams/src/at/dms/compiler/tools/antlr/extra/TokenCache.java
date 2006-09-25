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
 * $Id: TokenCache.java,v 1.3 2006-09-25 13:54:31 dimock Exp $
 */

package at.dms.compiler.tools.antlr.extra;

import java.util.Hashtable;

public class TokenCache {

    // --------------------------------------------------------------------
    // LOOKUP
    // --------------------------------------------------------------------

    public CToken lookupToken(int type, char[] data, int start, int length) {
        CToken  tok = null;

        currentToken.data = data;
        currentToken.start = start;
        currentToken.length = length;
        currentToken.type = type;

        tok = table.get(currentToken);
        if (tok == null) {
            tok = new CToken(type, String.valueOf(data, start, length).intern());
            table.put(currentToken.store(), tok);
        }

        return tok;
    }

    static class LookupToken {
        public char[] data;
        public int    start;
        public int    length;
        public int    type;

        public LookupToken store() {
            char[] temp = new char[length];
            LookupToken store = new LookupToken();

            System.arraycopy(data, start, temp, 0, length);
            store.data = temp;
            store.length = length;
            store.type = type;

            return store;
        }

        public int hashCode() {
            int h = 0;
            int index1 = start;
            char[]  val = data;

            for (int i = length - 1 ; i >= 0; i--) {
                h = (h * 37) + val[index1++];
            }

            return h * 37 + type;
        }

        public boolean equals(Object o) {
            if (!(o instanceof LookupToken)) {
                return false;
            }
            LookupToken tok = (LookupToken)o;

            if (length != tok.length || type != tok.type) {
                return false;
            }

            char[]  data1 = this.data;
            char[]  data2 = tok.data;
            int index1 = start;
            int index2 = tok.start;

            for (int i = 0; i < length; i++) {
                if (data1[index1++] != data2[index2++]) {
                    return false;
                }
            }

            return true;
        }
    }

    private LookupToken currentToken = new LookupToken();
    private Hashtable<LookupToken, CToken>   table = new Hashtable<LookupToken, CToken>(1000);
}
