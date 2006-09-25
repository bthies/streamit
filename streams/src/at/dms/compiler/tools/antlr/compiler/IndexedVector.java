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
 * $Id: IndexedVector.java,v 1.4 2006-09-25 13:54:31 dimock Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import java.util.Hashtable;
import java.util.Enumeration;

import at.dms.compiler.tools.antlr.runtime.*;

/**
 * A simple indexed vector: a normal vector except that you must
 * specify a key when adding an element.  This allows fast lookup
 * and allows the order of specification to be preserved.
 */
public class IndexedVector {
    protected Vector elements;
    protected Hashtable<Object, Object> index;


    /**
     * IndexedVector constructor comment.
     */
    public IndexedVector() {
        elements = new Vector(10);
        index = new Hashtable<Object, Object>(10);
    }
    /**
     * IndexedVector constructor comment.
     * @param size int
     */
    public IndexedVector(int size) {
        elements = new Vector(size);
        index = new Hashtable<Object, Object>(size);
    }
    public synchronized void appendElement(Object key, Object value) {
        elements.appendElement(value);
        index.put(key, value);
    }
    /**
     * Returns the element at the specified index.
     * @param i the index of the desired element
     * @exception ArrayIndexOutOfBoundsException If an invalid
     * index was given.
     */
    public Object elementAt(int i) {
        return elements.elementAt(i);
    }
    public Enumeration elements() {
        return elements.elements();
    }
    public Object getElement(Object key) {
        Object o = index.get(key);
        return o;
    }
    /**
     * remove element referred to by key NOT value; return false if not found.
     */
    public synchronized boolean removeElement(Object key) {
        Object value = index.get(key);
        if ( value == null ) {
            return false;
        }
        index.remove(key);
        elements.removeElement(value);
        return false;
    }
    public int size() {
        return elements.size();
    }
}
