/*
 * @(#)GraphSelectionEvent.java	1.0 1/1/02
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

package org.jgraph.event;

import java.util.EventObject;

/**
 * An event that characterizes a change in the current
 * selection.  The change is based on any number of cells.
 * GraphSelectionListeners will generally query the source of
 * the event for the new selected status of each potentially
 * changed cell.
 *
 * @see GraphSelectionListener
 * @see org.jgraph.graph.GraphSelectionModel
 *
 * @version 1.0 1/1/02
 * @author Gaudenz Alder
 */
public class GraphSelectionEvent extends EventObject {
	/** Cells this event represents. */
	protected Object[] cells;
	/** For each cell identifies if that cell is in fact new. */
	protected boolean[] areNew;

	/**
	  * Represents a change in the selection of a GraphSelectionModel.
	  * cells identifies the cells that have been either added or
	  * removed from the selection.
	  *
	  * @param source source of event
	  * @param cells the paths that have changed in the selection
	  */
	public GraphSelectionEvent(
		Object source,
		Object[] cells,
		boolean[] areNew) {
		super(source);
		this.cells = cells;
		this.areNew = areNew;
	}

	/**
	  * Returns the cells that have been added or removed from the
	  * selection.
	  */
	public Object[] getCells() {
		int numCells;
		Object[] retCells;

		numCells = cells.length;
		retCells = new Object[numCells];
		System.arraycopy(cells, 0, retCells, 0, numCells);
		return retCells;
	}

	/**
	  * Returns the first cell.
	  */
	public Object getCell() {
		return cells[0];
	}

	/**
	 * Returns true if the first cell has been added to the selection,
	 * a return value of false means the first cell has been
	 * removed from the selection.
	 */
	public boolean isAddedCell() {
		return areNew[0];
	}

	/**
	 * Returns true if the cell identified by cell was added to the
	 * selection. A return value of false means the cell was in the
	 * selection but is no longer in the selection. This will raise if
	 * cell is not one of the cells identified by this event.
	 */
	public boolean isAddedCell(Object cell) {
		for (int counter = cells.length - 1; counter >= 0; counter--)
			if (cells[counter].equals(cell))
				return areNew[counter];
		throw new IllegalArgumentException("cell is not a cell identified by the GraphSelectionEvent");
	}

	/**
	 * Returns true if the cell identified by <code>index</code> was added to
	 * the selection. A return value of false means the cell was in the
	 * selection but is no longer in the selection. This will raise if
	 * index < 0 || >= <code>getPaths</code>.length.
	 *
	 * @since 1.3
	 */
	public boolean isAddedCell(int index) {
		if (cells == null || index < 0 || index >= cells.length) {
			throw new IllegalArgumentException("index is beyond range of added cells identified by GraphSelectionEvent");
		}
		return areNew[index];
	}

	/**
	 * Returns a copy of the receiver, but with the source being newSource.
	 */
	public Object cloneWithSource(Object newSource) {
		// Fix for IE bug - crashing
		return new GraphSelectionEvent(newSource, cells, areNew);
	}
}