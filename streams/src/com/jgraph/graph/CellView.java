/*
 * @(#)CellView.java	1.0 1/1/02
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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Map;

import com.jgraph.JGraph;

/**
 * Defines the requirements for an object that
 * represents a view for a model cell.
 *
 * @version 1.0 1/1/02
 * @author Gaudenz Alder
 */

public interface CellView {

	//
	// Data Source
	//

	/**
	 * Returns the model object that this view represents.
	 */
	Object getCell();

	/**
	 * Refresh this view based on the model cell. This is
	 * messaged when the model cell has changed.
	 */
	void refresh(boolean createDependentViews);

	/**
	 * Update this view's attributes. This is messaged whenever refresh is
	 * messaged, and additionally when the context of the cell has changed,
	 * and during live-preview changes to the view.
	 */
	void update();

	void childUpdated();

	//
	// Group Structure
	//

	/**
	 * Returns the parent of view of this view.
	 */
	CellView getParentView();

	/**
	 * Returns the child views of this view.
	 */
	CellView[] getChildViews();

	/**
	 * Removes this view from the list of childs of the parent.
	 */
	void removeFromParent();

	/**
	 * Returns true if the view is a leaf.
	 */
	boolean isLeaf();

	//
	// View Methods
	//

	/**
	 * Returns the bounds for the view.
	 */
	Rectangle getBounds();

	/**
	 * Returns true if the view intersects the given rectangle.
	 */
	boolean intersects(Graphics g, Rectangle rect);

	/**
	 * Apply the specified map of attributes on the view.
	 */
	Map setAttributes(Map map);

	/**
	 * Returns all attributes of the view as a map.
	 */
	Map getAttributes();

	Map getAllAttributes();

	//
	// Renderer, Editor and Handle
	//

	/**
	 * Returns a renderer component, configured for the view.
	 */
	Component getRendererComponent(
		JGraph graph,
		boolean selected,
		boolean focus,
		boolean preview);

	/**
	 * Returns a cell handle for the view.
	 */
	CellHandle getHandle(GraphContext context);

	/**
	 * Returns a cell editor for the view.
	 */
	GraphCellEditor getEditor();

}