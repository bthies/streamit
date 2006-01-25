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
 * $Id: LLkParser.java,v 1.3 2006-01-25 17:00:46 thies Exp $
 */

package at.dms.compiler.antlr.runtime;

import java.io.IOException;

/**
 * An LL(k) parser.
 *
 * @see at.dms.compiler.antlr.runtime.Token
 * @see at.dms.compiler.antlr.runtime.TokenBuffer
 */
public class LLkParser extends Parser {
    int k;

    public LLkParser(int k_) {
        k = k_;
        //TokenBuffer tokenBuf = new TokenBuffer(null);
        //setTokenBuffer(tokenBuf);
    }
    public LLkParser(ParserSharedInputState state, int k_) {
        k = k_;
        inputState = state;
    }
    public LLkParser(TokenBuffer tokenBuf, int k_) {
        k = k_;
        setTokenBuffer(tokenBuf);
    }
    public LLkParser(TokenStream lexer, int k_) {
        k = k_;
        TokenBuffer tokenBuf = new TokenBuffer(lexer);
        setTokenBuffer(tokenBuf);
    }
    /**
     * Consume another token from the input stream.  Can only write sequentially!
     * If you need 3 tokens ahead, you must consume() 3 times.
     * <p>
     * Note that it is possible to overwrite tokens that have not been matched.
     * For example, calling consume() 3 times when k=2, means that the first token
     * consumed will be overwritten with the 3rd.
     */
    public void consume() {
        inputState.input.consume();
    }
    public int LA(int i) throws TokenStreamException {
        return inputState.input.LA(i);
    }
    public Token LT(int i) throws TokenStreamException {
        return inputState.input.LT(i);
    }
}
