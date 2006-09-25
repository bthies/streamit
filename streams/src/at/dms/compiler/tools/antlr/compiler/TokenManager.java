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
 * $Id: TokenManager.java,v 1.3 2006-09-25 13:54:31 dimock Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import java.util.Enumeration;

import at.dms.compiler.tools.antlr.runtime.*;

/**
 * Interface that describes the set of defined tokens
 */
interface TokenManager {
    Object clone();

    /**
     * define a token symbol
     */
    void define(TokenSymbol ts);

    /**
     * Get the name of the token manager
     */
    String getName();

    /**
     * Get a token string by index
     */
    String getTokenStringAt(int idx);

    /**
     * Get the TokenSymbol for a string
     */
    TokenSymbol getTokenSymbol(String sym);
    TokenSymbol getTokenSymbolAt(int idx);

    /**
     * Get an enumerator over the symbol table
     */
    Enumeration<TokenSymbol> getTokenSymbolElements();
    Enumeration<String> getTokenSymbolKeys();

    /**
     * Get the token vocabulary (read-only).
     * @return A Vector of Strings indexed by token type */
    Vector getVocabulary();

    /**
     * Is this token manager read-only?
     */
    boolean isReadOnly();

    void mapToTokenSymbol(String name, TokenSymbol sym);

    /**
     * Get the highest token type in use
     */
    int maxTokenType();

    /**
     * Get the next unused token type
     */
    int nextTokenType();

    void setName(String n);

    void setReadOnly(boolean ro);

    /**
     * Is a token symbol defined?
     */
    boolean tokenDefined(String symbol);
}
