/*
 * @(#)CellMapper.java	1.0 1/1/02
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

/**
 * Defines the requirements for objects that may be used as
 * a cell mapper.
 *
 * @version 1.0 1/1/02
 * @author Gaudenz Alder
 */

public interface CellMapper {

	/**
	 * Returns the view that is associated with <code>cell</code>.
	 *
	 * @param create whether a new view should created
	 */
	CellView getMapping(Object cell, boolean create);

	/**
	 * Inserts the association between <code>cell</code> and <code>view</code>.
	 *
	 * @param cell the cell that constitutes the model element
	 * @param view the view that constitutes the view element
	 */
	void putMapping(Object cell, CellView view);

}