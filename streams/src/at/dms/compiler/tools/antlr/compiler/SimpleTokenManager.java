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
 * $Id: SimpleTokenManager.java,v 1.3 2006-09-25 13:54:31 dimock Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import java.util.Hashtable;
import java.util.Enumeration;

import at.dms.compiler.tools.antlr.runtime.*;

class SimpleTokenManager implements TokenManager, Cloneable {
    protected int maxToken = Token.MIN_USER_TYPE;
    // Token vocabulary is Vector of String's
    protected Vector vocabulary;
    // Hash table is a mapping from Strings to TokenSymbol
    private Hashtable<String, TokenSymbol> table;
    // the main class
    protected Main tool;
    // Name of the token manager
    protected String name;

    protected boolean readOnly = false;

    SimpleTokenManager(String name_, Main tool_) {
        tool = tool_;
        name = name_;
        // Don't make a bigger vector than we need, because it will show up in output sets.
        vocabulary = new Vector(1);
        table = new Hashtable<String, TokenSymbol>();

        // define EOF symbol
        TokenSymbol ts = new TokenSymbol("EOF");
        ts.setTokenType(Token.EOF_TYPE);
        define(ts);

        // define <null-tree-lookahead> but only in the vocabulary vector
        vocabulary.ensureCapacity(Token.NULL_TREE_LOOKAHEAD);
        vocabulary.setElementAt("NULL_TREE_LOOKAHEAD", Token.NULL_TREE_LOOKAHEAD);
    }

    public Object clone() {
        SimpleTokenManager tm;
        try {
            tm = (SimpleTokenManager) super.clone();
            tm.vocabulary = (Vector) this.vocabulary.clone();
            tm.table = (Hashtable<String, TokenSymbol>) this.table.clone();
            tm.maxToken = this.maxToken;
            tm.tool = this.tool;
            tm.name = this.name;
        } catch (CloneNotSupportedException e) {
            Utils.panic("cannot clone token manager");
            return null;
        }
        return tm;
    }
    /**
     * define a token
     */
    public void define(TokenSymbol ts) {
        // Add the symbol to the vocabulary vector
        vocabulary.ensureCapacity(ts.getTokenType());
        vocabulary.setElementAt(ts.getId(), ts.getTokenType());
        // add the symbol to the hash table
        mapToTokenSymbol(ts.getId(), ts);
    }
    /**
     * Simple token manager doesn't have a name -- must be set externally
     */
    public String getName() { return name; }
    /**
     * Get a token symbol by index
     */
    public String getTokenStringAt(int idx) {
        return (String)vocabulary.elementAt(idx);
    }
    /**
     * Get the TokenSymbol for a string
     */
    public TokenSymbol getTokenSymbol(String sym) {
        return table.get(sym);
    }
    /**
     * Get a token symbol by index
     */
    public TokenSymbol getTokenSymbolAt(int idx) {
        return getTokenSymbol(getTokenStringAt(idx));
    }
    /**
     * Get an enumerator over the symbol table
     */
    public Enumeration<TokenSymbol> getTokenSymbolElements() {
        return table.elements();
    }
    public Enumeration<String> getTokenSymbolKeys() {
        return table.keys();
    }
    /**
     * Get the token vocabulary (read-only).
     * @return A Vector of TokenSymbol
     */
    public Vector getVocabulary() {
        return vocabulary;
    }
    /**
     * Simple token manager is not read-only
     */
    public boolean isReadOnly() { return false; }
    /**
     * Map a label or string to an existing token symbol
     */
    public void mapToTokenSymbol(String name, TokenSymbol sym) {
        table.put(name, sym);
    }
    /**
     * Get the highest token type in use
     */
    public int maxTokenType() {
        return maxToken-1;
    }
    /**
     * Get the next unused token type
     */
    public int nextTokenType() {
        return maxToken++;
    }
    /**
     * Set the name of the token manager
     */
    public void setName(String name_) { name = name_; }

    public void setReadOnly(boolean ro) {
        readOnly = ro;
    }
    /**
     * Is a token symbol defined?
     */
    public boolean tokenDefined(String symbol) {
        return table.containsKey(symbol);
    }
}
