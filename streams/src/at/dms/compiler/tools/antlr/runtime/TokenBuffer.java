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
 * $Id: TokenBuffer.java,v 1.3 2006-03-23 18:53:36 dimock Exp $
 */

package at.dms.compiler.tools.antlr.runtime;

/**
 * A Stream of Token objects fed to the parser from a Tokenizer that can
 * be rewound via mark()/rewind() methods.
 * <p>
 * A dynamic array is used to buffer up all the input tokens.  Normally,
 * "k" tokens are stored in the buffer.  More tokens may be stored during
 * guess mode (testing syntactic predicate), or when LT(i&gt;k) is referenced.
 * Consumption of tokens is deferred.  In other words, reading the next
 * token is not done by conume(), but deferred until needed by LA or LT.
 * <p>
 *
 * @see at.dms.compiler.tools.antlr.runtime.Token
 * @see at.dms.compiler.tools.antlr.runtime.TokenQueue
 */

import java.io.IOException;

public class TokenBuffer {

    // Token source
    protected TokenStream input;

    // Number of active markers
    int nMarkers = 0;

    // Additional offset used when markers are active
    int markerOffset = 0;

    // Number of calls to consume() since last LA() or LT() call
    int numToConsume = 0;

    // Circular queue
    TokenQueue queue;


    /**
     * Create a token buffer
     */
    public TokenBuffer(TokenStream input_) {
        input = input_;
        queue = new TokenQueue(1);
    }
    /**
     * Mark another token for deferred consumption
     */
    public final void consume() {
        numToConsume++;
    }
    /**
     * Ensure that the token buffer is sufficiently full
     */
    private final void fill(int amount) throws TokenStreamException
    {
        syncConsume();
        // Fill the buffer sufficiently to hold needed tokens
        while (queue.nbrEntries < amount + markerOffset) {
            // Append the next token
            queue.append(input.nextToken());
        }
    }
    /**
     * return the Tokenizer (needed by ParseView)
     */
    public TokenStream getInput() {
        return input;
    }
    /**
     * Get a lookahead token value
     */
    public final int LA(int i) throws TokenStreamException {
        fill(i);
        return queue.elementAt(markerOffset + i - 1).type;
    }
    /**
     * Get a lookahead token
     */
    public final Token LT(int i) throws TokenStreamException {
        fill(i);
        return queue.elementAt(markerOffset + i - 1);
    }
    /**
     * Return an integer marker that can be used to rewind the buffer to
     * its current state.
     */
    public final int mark() {
        syncConsume();
        //System.out.println("Marking at " + markerOffset);
        //try { for (int i = 1; i <= 2; i++) { System.out.println("LA("+i+")=="+LT(i).getText()); } } catch (ScannerException e) {}
        nMarkers++;
        return markerOffset;
    }
    /**
     * Rewind the token buffer to a marker.
     * @param mark Marker returned previously from mark()
     */
    public final void rewind(int mark) {
        syncConsume();
        markerOffset = mark;
        nMarkers--;
        //System.out.println("Rewinding to " + mark);
        //try { for (int i = 1; i <= 2; i++) { System.out.println("LA("+i+")=="+LT(i).getText()); } } catch (ScannerException e) {}
    }
    /**
     * Sync up deferred consumption
     */
    private final void syncConsume() {
        while (numToConsume > 0) {
            if (nMarkers > 0) {
                // guess mode -- leave leading tokens and bump offset.
                markerOffset++;
            } else {
                // normal mode -- remove first token
                queue.removeFirst();
            }
            numToConsume--;
        }
    }
}
