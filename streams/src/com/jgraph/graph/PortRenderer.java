/*
 * @(#)PortRenderer.java	1.0 28/11/01
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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.io.Serializable;

import javax.swing.JComponent;
import javax.swing.UIManager;

import com.jgraph.JGraph;

/**
 * This renderer displays entries that implement the CellView interface
 * and supports the following attributes:
 * <li>
 * GraphConstants.OFFSET
 * GraphConstants.ABSOLUTE
 * </li
 * @version 1.0 28/11/01
 * @author Gaudenz Alder
 */

public class PortRenderer
	extends JComponent
	implements CellViewRenderer, Serializable {

	/** Cache the current graph for drawing */
	protected transient JGraph graph;

	/** Cache the current edgeview for drawing. */
	protected transient PortView view;

	/** Cached hasFocus and selected value. */
	transient protected boolean hasFocus, selected, preview;

	/**
	 * Constructs a renderer that may be used to render ports.
	 */
	public PortRenderer() {
		setForeground(UIManager.getColor("MenuItem.selectionBackground"));
		setBackground(UIManager.getColor("Tree.selectionBorderColor"));
	}

	/**
	 * Configure and return the renderer based on the passed in
	 * components. The value is typically set from messaging the
	 * graph with <code>convertValueToString</code>.
	 *
	 * @param   graph the graph that that defines the rendering context.
	 * @param   value the object that should be rendered.
	 * @param   selected whether the object is selected.
	 * @param   hasFocus whether the object has the focus.
	 * @param   isPreview whether we are drawing a preview.
	 * @return	the component used to render the value.
	 */
	public Component getRendererComponent(
		JGraph graph,
		CellView view,
		boolean sel,
		boolean focus,
		boolean preview) {
		// Check type
		if (view instanceof PortView && graph != null) {
			this.graph = graph;
			this.view = (PortView) view;
			this.hasFocus = focus;
			this.selected = sel;
			this.preview = preview;
			return this;
		}
		return null;
	}

	/**
	 * Paint the renderer. Overrides superclass paint
	 * to add specific painting. Note: The preview flag
	 * is interpreted as "highlight" in this context. (This
	 * is used to highlight the port if the mouse is over it.)
	 */
	public void paint(Graphics g) {
		Dimension d = getSize();
		g.setColor(graph.getBackground());
		g.setXORMode(graph.getBackground());
		if (preview) {
			g.setColor(getBackground());
			g.drawRect(1, 1, d.width - 3, d.height - 3);
			g.drawRect(2, 2, d.width - 5, d.height - 5);
		}
		boolean offset =
			(GraphConstants.getOffset(view.getAllAttributes()) != null);
		g.setColor(getForeground());
		if (offset) {
			g.drawLine(2, 2, d.width - 2, d.height - 2);
			g.drawLine(2, d.width - 2, d.height - 2, 2);
		} else if (!preview)
			g.drawRect(3, 3, d.width - 4, d.height - 4);
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void validate() {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void revalidate() {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void repaint(long tm, int x, int y, int width, int height) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void repaint(Rectangle r) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	protected void firePropertyChange(
		String propertyName,
		Object oldValue,
		Object newValue) {
		// Strings get interned...
		if (propertyName == "text")
			super.firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		byte oldValue,
		byte newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		char oldValue,
		char newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		short oldValue,
		short newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		int oldValue,
		int newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		long oldValue,
		long newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		float oldValue,
		float newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		double oldValue,
		double newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		boolean oldValue,
		boolean newValue) {
	}

}