/*
 * @(#)CellHandle.java	1.0 1/1/02
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

import java.awt.Graphics;
import java.awt.event.MouseEvent;

/**
 * Defines the requirements for objects that may be used as handles.
 * Handles are used to interactively manipulate a cell's appearance.
 *
 * @version 1.0 1/1/02
 * @author Gaudenz Alder
 */

public interface CellHandle {

	/**
	 * Paint the handle on the given graphics object once.
	 *
	 * @param g       the graphics object to paint the handle on
	 */
	void paint(Graphics g);

	/**
	 * Paint the handle on the given graphics object during mouse
	 * operations.
	 *
	 * @param g       the graphics object to paint the handle on
	 */
	void overlay(Graphics g);

	/**
	 * Return a cursor for the given point.
	 *
	 * @param p   the point for which the cursor is returned
	 */
	void mouseMoved(MouseEvent e);

	/**
	 * Messaged when a drag gesture is recogniced.
	 *
	 * @param e   the drag gesture event to be processed
	 */
	void mousePressed(MouseEvent event);

	/**
	 * Messagedwhen the user drags the selection.
	 * The Controller is responsible to determine whether the mouse is
	 * inside the parent graph or not.
	 *
	 * @param e   the drag event to be processed
	 */
	void mouseDragged(MouseEvent e);

	/**
	 * Messaged when the drag operation has
	 * terminated with a drop.
	 *
	 * @param e   the drop event to be processed
	 */
	void mouseReleased(MouseEvent event);

}