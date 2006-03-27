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
 * $Id: InputBuffer.java,v 1.4 2006-03-27 21:42:55 dimock Exp $
 */

package at.dms.compiler.tools.antlr.runtime;

// SAS: Added this class to genericise the input buffers for scanners
//      This allows a scanner to use a binary (FileInputStream) or
//      text (FileReader) stream of data; the generated scanner
//      subclass will define the input stream
//      There are two subclasses to this: CharBuffer and ByteBuffer

import java.io.IOException;

/**
 * A Stream of characters fed to the lexer from a InputStream that can
 * be rewound via mark()/rewind() methods.
 * <p>
 * A dynamic array is used to buffer up all the input characters.  Normally,
 * "k" characters are stored in the buffer.  More characters may be stored during
 * guess mode (testing syntactic predicate), or when LT(i&gt;k) is referenced.
 * Consumption of characters is deferred.  In other words, reading the next
 * character is not done by conume(), but deferred until needed by LA or LT.
 * <p>
 *
 * @see at.dms.compiler.tools.antlr.runtime.CharQueue
 */
public abstract class InputBuffer {
    // char source
    //  transient Reader input;  // leave to subclasses

    // Number of active markers
    protected int nMarkers = 0;

    // Additional offset used when markers are active
    protected int markerOffset = 0;

    // Number of calls to consume() since last LA() or LT() call
    protected int numToConsume = 0;

    // Circular queue
    protected CharQueue queue;

    /**
     * Create an input buffer
     */
    public InputBuffer() {
        queue = new CharQueue(1);
    }
    /**
     * This method updates the state of the input buffer so that
     *  the text matched since the most recent mark() is no longer
     *  held by the buffer.  So, you either do a mark/rewind for
     *  failed predicate or mark/commit to keep on parsing without
     *  rewinding the input.
     */
    public void commit() {
        nMarkers--;
    }
    /**
     * Mark another character for deferred consumption
     */
    public void consume() {
        numToConsume++;
    }
    /**
     * Ensure that the input buffer is sufficiently full
     */
    public abstract void fill(int amount) throws CharStreamException;
    public String getLAChars() {
        StringBuffer la = new StringBuffer();
        for(int i = markerOffset; i < queue.nbrEntries; i++)
            la.append(queue.elementAt(i));
        return la.toString();
    }
    public String getMarkedChars() {
        StringBuffer marked = new StringBuffer();
        for(int i = 0; i < markerOffset; i++)
            marked.append(queue.elementAt(i));
        return marked.toString();
    }
    public boolean isMarked() {
        return (nMarkers != 0);
    }
    /**
     * Get a lookahead character
     */
    public char LA(int i) throws CharStreamException {
        fill(i);
        return queue.elementAt(markerOffset + i - 1);
    }
    /**
     * Return an integer marker that can be used to rewind the buffer to
     * its current state.
     */
    public int mark() {
        syncConsume();
        nMarkers++;
        return markerOffset;
    }
    /**
     * Rewind the character buffer to a marker.
     * @param mark Marker returned previously from mark()
     */
    public void rewind(int mark) {
        syncConsume();
        markerOffset = mark;
        nMarkers--;
    }
    /**
     * Sync up deferred consumption
     */
    protected void syncConsume() {
        while (numToConsume > 0) {
            if (nMarkers > 0) {
                // guess mode -- leave leading characters and bump offset.
                markerOffset++;
            } else {
                // normal mode -- remove first character
                queue.removeFirst();
            }
            numToConsume--;
        }
    }
}
