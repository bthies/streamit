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
 * $Id: Parser.java,v 1.4 2006-03-23 18:30:50 dimock Exp $
 */

package at.dms.compiler.antlr.runtime;

/**
 * A generic ANTLR parser (LL(k) for k&gt;=1) containing a bunch of
 * utility routines useful at any lookahead depth.  We distinguish between
 * the LL(1) and LL(k) parsers because of efficiency.  This may not be
 * necessary in the near future.
 *
 * Each parser object contains the state of the parse including a lookahead
 * cache (the form of which is determined by the subclass), whether or
 * not the parser is in guess mode, where tokens come from, etc...
 *
 * <p>
 * During <b>guess</b> mode, the current lookahead token(s) and token type(s)
 * cache must be saved because the token stream may not have been informed
 * to save the token (via <tt>mark</tt>) before the <tt>try</tt> block.
 * Guessing is started by:
 * <ol>
 * <li>saving the lookahead cache.
 * <li>marking the current position in the TokenBuffer.
 * <li>increasing the guessing level.
 * </ol>
 *
 * After guessing, the parser state is restored by:
 * <ol>
 * <li>restoring the lookahead cache.
 * <li>rewinding the TokenBuffer.
 * <li>decreasing the guessing level.
 * </ol>
 *
 * @see at.dms.compiler.antlr.runtime.Token
 * @see at.dms.compiler.antlr.runtime.TokenBuffer
 * @see at.dms.compiler.antlr.runtime.Tokenizer
 * @see at.dms.compiler.antlr.runtime.LLkParser
 */

import java.io.IOException;

public abstract class Parser {
    protected ParserSharedInputState inputState;

    /**
     * Nesting level of registered handlers
     */
    // protected int exceptionLevel = 0;

    /**
     * Table of token type to token names
     */
    protected String[] tokenNames;

    public Parser() {
        inputState = new ParserSharedInputState();
    }

    public Parser(ParserSharedInputState state) {
        inputState = state;
    }

    /**
     * Get another token object from the token stream
     */
    public abstract void consume() throws TokenStreamException;

    /**
     * Consume tokens until one matches the given token
     */
    public void consumeUntil(int tokenType) throws TokenStreamException {
        while (LA(1) != Token.EOF_TYPE && LA(1) != tokenType)  {
            consume();
        }
    }

    /**
     * Consume tokens until one matches the given token set
     */
    public void consumeUntil(BitSet set) throws TokenStreamException {
        while (LA(1) != Token.EOF_TYPE && !set.member(LA(1))) {
            consume();
        }
    }

    protected void defaultDebuggingSetup(TokenStream lexer, TokenBuffer tokBuf) {
        // by default, do nothing -- we're not debugging
    }

    public String getFilename() {return inputState.filename;}

    public ParserSharedInputState getInputState() {
        return inputState;
    }

    public void setInputState(ParserSharedInputState state) {
        inputState = state;
    }

    public String getTokenName(int num) {
        return tokenNames[num];
    }
    public String[] getTokenNames() {
        return tokenNames;
    }

    /**
     * Return the token type of the ith token of lookahead where i=1
     * is the current token being examined by the parser (i.e., it
     * has not been matched yet).
     */
    public abstract int LA(int i) throws TokenStreamException;
    /**
     * Return the ith token of lookahead
     */
    public abstract Token LT(int i) throws TokenStreamException;
    // Forwarded to TokenBuffer
    public int mark() {
        return inputState.input.mark();
    }
    /**
     * Make sure current lookahead symbol matches token type <tt>t</tt>.
     * Throw an exception upon mismatch, which is catch by either the
     * error handler or by the syntactic predicate.
     */
    public void match(int t) throws MismatchedTokenException, TokenStreamException {
        if ( LA(1)!=t ) {
            throw new MismatchedTokenException(tokenNames, LT(1), t, false, getFilename());
        } else {
            // mark token as consumed -- fetch next token deferred until LA/LT
            consume();
        }
    }
    /**
     * Make sure current lookahead symbol matches the given set
     * Throw an exception upon mismatch, which is catch by either the
     * error handler or by the syntactic predicate.
     */
    public void match(BitSet b) throws MismatchedTokenException, TokenStreamException {
        if ( !b.member(LA(1)) ) {
            throw new MismatchedTokenException(tokenNames, LT(1), b, false, getFilename());
        } else {
            // mark token as consumed -- fetch next token deferred until LA/LT
            consume();
        }
    }
    public void matchNot(int t) throws MismatchedTokenException, TokenStreamException {
        if ( LA(1)==t ) {
            // Throws inverted-sense exception
            throw new MismatchedTokenException(tokenNames, LT(1), t, true, getFilename());
        } else {
            // mark token as consumed -- fetch next token deferred until LA/LT
            consume();
        }
    }
    public static void panic() {
        System.err.println("Parser: panic");
        System.exit(1);
    }

    /**
     * Parser error-reporting function can be overridden in subclass
     */
    public void reportError(RecognitionException ex) {
        System.err.println(ex);
    }

    /**
     * Parser error-reporting function can be overridden in subclass
     */
    public void reportError(String s) {
        if ( getFilename()==null ) {
            System.err.println("error: " + s);
        } else {
            System.err.println(getFilename()+": error: " + s);
        }
    }

    /**
     * Parser warning-reporting function can be overridden in subclass
     */
    public void reportWarning(String s) {
        if ( getFilename()==null ) {
            System.err.println("warning: "+s);
        } else {
            System.err.println(getFilename()+": warning: " + s);
        }
    }

    public void rewind(int pos) {
        inputState.input.rewind(pos);
    }

    public void setFilename(String f) {inputState.filename=f;}

    /**
     * Set or change the input token buffer
     */
    public void setTokenBuffer(TokenBuffer t) { inputState.input = t; }
}
