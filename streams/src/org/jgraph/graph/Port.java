/*
 * @(#)Port.java	1.0 1/1/02
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

package org.jgraph.graph;

import java.util.Iterator;

/**
 * Defines the requirements for an object that
 * represents a port in a graph model.
 *
 * @version 1.0 1/1/02
 * @author Gaudenz Alder
 */

public interface Port extends GraphCell {

	/**
	 * Returns an iterator of the edges connected
	 * to the port.
	 */
	Iterator edges();

	/**
	 * Adds <code>edge</code> to the list of ports.
	 */
	boolean addEdge(Object edge);

	/**
	 * Removes <code>edge</code> from the list of ports.
	 */
	boolean removeEdge(Object edge);

	/**
	 * Returns the anchor of the port.
	 */
	Port getAnchor();

	/**
	 * Sets the anchor of the port.
	 */
	void setAnchor(Port port);

}