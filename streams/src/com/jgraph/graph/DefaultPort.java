/*
 * @(#)DefaultPort.java	1.0 1/1/02
 *
 * Copyright (C) 2001 Gaudenz Alder
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
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
 */

package com.jgraph.graph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A simple implementation for a port.
 *
 * @version 1.0 1/1/02
 * @author Gaudenz Alder
 */

public class DefaultPort extends DefaultGraphCell implements Port {

	/** Edges that are connected to the port */
	protected HashSet edges = new HashSet();

	/** Reference to the anchor of this port */
	protected Port anchor;

	/**
	 * Constructs an empty port.
	 */
	public DefaultPort() {
		this(null, null);
	}

	/**
	 * Constructs a vertex that holds a reference to the specified user object.
	 *
	 * @param userObject reference to the user object
	 */
	public DefaultPort(Object userObject) {
		this(userObject, null);
	}

	/**
	 * Constructs a vertex that holds a reference to the specified user object
	 * and a reference to the specified anchor.
	 *
	 * @param userObject reference to the user object
	 * @param reference to a a graphcell that constitutes the anchor
	 */
	public DefaultPort(Object userObject, Port anchor) {
		super(userObject, false);
		this.anchor = anchor;
	}

	/**
	 * Returns an iterator of the edges connected
	 * to the port.
	 */
	public Iterator edges() {
		return edges.iterator();
	}

	/**
	 * Adds <code>edge</code> to the list of ports.
	 */
	public boolean addEdge(Object edge) {
		return edges.add(edge);
	}

	/**
	 * Removes <code>edge</code> from the list of ports.
	 */
	public boolean removeEdge(Object edge) {
		return edges.remove(edge);
	}

	/**
	 * Returns the anchor of this port.
	 */
	public Set getEdges() {
		return new HashSet(edges);
	}

	/**
	 * Sets the anchor of this port.
	 */
	public void setEdges(Set edges) {
		this.edges = new HashSet(edges);
	}

	/**
	 * Returns the anchor of this port.
	 */
	public Port getAnchor() {
		return anchor;
	}

	/**
	 * Sets the anchor of this port.
	 */
	public void setAnchor(Port port) {
		anchor = port;
	}

	/**
	 * Create a clone of the cell. The cloning of the
	 * user object is deferred to the cloneUserObject()
	 * method.
	 *
	 * @return Object  a clone of this object.
	 */
	public Object clone() {
		DefaultPort c = (DefaultPort) super.clone();
		c.edges = new HashSet();
		return c;
	}

}