/*
 * @(#)GraphCell.java	1.0 1/1/02
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

import java.util.Map;

/**
 * Defines the requirements for objects that appear as
 * GraphCells. This is the base interface for all GraphCells.
 *
 * @version 1.0 1/1/02
 * @author Gaudenz Alder
 */

public interface GraphCell {

	/**
	 * Returns the <code>attributes</code> of the cell.
	 */
	Map getAttributes();

	/**
	 * Changes the <code>attributes</code> of the cell.
	 */
	Map changeAttributes(Map properties);

	/**
	 * Sets the attributes
	 */
	public void setAttributes(Map map);

}