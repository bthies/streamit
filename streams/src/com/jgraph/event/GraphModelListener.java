/*
 * @(#)GraphModelListener.java	1.0 1/1/02
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

package com.jgraph.event;

import java.util.EventListener;

/**
 * Defines the interface for an object that listens
 * to changes in a GraphModel.
 *
 * @author Gaudenz Alder
 * @version 1.0 1/1/02
 */
public interface GraphModelListener extends EventListener {

	/**
	 * Invoked after a cell has changed in some way.
	 * The vertex/vertices may have changed bounds or
	 * altered adjacency, or other attributes have
	 * changed that may affect presentation.
	 */
	void graphChanged(GraphModelEvent e);

}