/*
 * @(#)GraphUI.java	1.0 1/1/02
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

package org.jgraph.plaf;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.plaf.ComponentUI;

import org.jgraph.JGraph;
import org.jgraph.graph.CellHandle;
import org.jgraph.graph.CellView;

/**
 * Pluggable look and feel interface for JGraph.
 *
 * @version 1.0 1/1/02
 * @author Gaudenz Alder
 */
public abstract class GraphUI extends ComponentUI {

	/**
	 * Paints the renderer of <code>view</code> to <code>g</code>
	 * at <code>bounds</code>.
	 */
	public abstract void paintCell(
		Graphics g,
		CellView view,
		Rectangle bounds,
		boolean preview);

	/**
	 * Paints the renderers of <code>portViews</code> to <code>g</code>.
	 */
	public abstract void paintPorts(Graphics g, CellView[] portViews);

	/**
	 * Messaged to update the selection based on a MouseEvent for a group of
	 * cells. If the event is a toggle selection event, the cells are either
	 * selected, or deselected. Otherwise the cells are selected.
	 */
	public abstract void selectCellsForEvent(
		JGraph graph,
		Object[] cells,
		MouseEvent event);

	/**
	  * Returns the preferred size for <code>view</code>.
	  */
	public abstract Dimension getPreferredSize(JGraph graph, CellView view);

	/**
	  * Returns the <code>CellHandle</code> that is currently active,
	  * or <code>null</code> if no handle is active.
	  */
	public abstract CellHandle getHandle(JGraph graph);

	/**
	  * Returns true if the graph is being edited.  The item that is being
	  * edited can be returned by getEditingCell().
	  */
	public abstract boolean isEditing(JGraph graph);

	/**
	  * Stops the current editing session.  This has no effect if the
	  * graph isn't being edited.  Returns true if the editor allows the
	  * editing session to stop.
	  */
	public abstract boolean stopEditing(JGraph graph);

	/**
	  * Cancels the current editing session. This has no effect if the
	  * graph isn't being edited.  Returns true if the editor allows the
	  * editing session to stop.
	  */
	public abstract void cancelEditing(JGraph graph);

	/**
	  * Selects the cell and tries to edit it.  Editing will
	  * fail if the CellEditor won't allow it for the selected item.
	  */
	public abstract void startEditingAtCell(JGraph graph, Object cell);

	/**
	 * Returns the cell that is being edited.
	 */
	public abstract Object getEditingCell(JGraph graph);

}