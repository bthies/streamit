/*
 * @(#)GraphCellEditor.java	1.0 1/1/02
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

import java.awt.Component;

import javax.swing.CellEditor;

import org.jgraph.JGraph;

/**
  * Adds to CellEditor the extensions necessary to configure an editor
  * in a graph.
  *
  * @version 1.0 1/1/02
  * @author Gaudenz Alder
  */

public interface GraphCellEditor extends CellEditor {
	/**
	 * Sets an initial <I>value</I> for the editor.  This will cause
	 * the editor to stopEditing and lose any partially edited value
	 * if the editor is editing when this method is called. <p>
	 *
	 * Returns the component that should be added to the client's
	 * Component hierarchy.  Once installed in the client's hierarchy
	 * this component will then be able to draw and receive user input.
	 *
	 * @param	graph		the JGraph that is asking the editor to edit
	 *				This parameter can be null.
	 * @param	value		the value of the cell to be edited.
	 * @param	isSelected	true if the cell is to be rendered with
	 *				selection highlighting
	 * @return	the component for editing
	 */
	Component getGraphCellEditorComponent(
		JGraph graph,
		Object value,
		boolean isSelected);
}