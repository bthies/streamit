/*
 * @(#)BasicMarqueeHandler.java	1.0 1/3/02
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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import org.jgraph.JGraph;

/**
 * A simple implementation of a marquee handler for JGraph.
 *
 * @version 1.0 1/1/02
 * @author Gaudenz Alder
 */

public class BasicMarqueeHandler {

	/* Restore previous cursor after operation. */
	protected transient Cursor previousCursor = null;

	/* The rectangle that defines the current marquee selection. */
	protected Rectangle marqueeBounds;

	/* The start start and current point of the marquee session. */
	protected Point startPoint, currentPoint;

	/* Return true if this handler should be preferred over other handlers. */
	public boolean isForceMarqueeEvent(MouseEvent event) {
		return event.isAltDown();
	}

	/**
	  * Stops the current marquee selection.
	  */
	public void mouseReleased(MouseEvent e) {
	
		try {
			if (e != null && !e.isConsumed() && marqueeBounds != null) {
				if (!(e.getSource() instanceof JGraph))
					throw new IllegalArgumentException(
						"MarqueeHandler cannot "
							+ "handle event from unknown source: "
							+ e);
				JGraph graph = (JGraph) e.getSource();
				Rectangle bounds =
					graph.fromScreen(new Rectangle(marqueeBounds));
				CellView[] views = graph.getGraphLayoutCache().getRoots(bounds);
				ArrayList list = new ArrayList();
				for (int i = 0; i < views.length; i++)
					if (bounds.contains(views[i].getBounds()))
						// above returns intersection
						list.add(views[i].getCell());
				Object[] cells = list.toArray();
				graph.getUI().selectCellsForEvent(graph, cells, e);
				graph.setCursor(previousCursor);
				Rectangle dirty = new Rectangle(marqueeBounds);
				dirty.width++;
				dirty.height++;
				graph.repaint(dirty);
				e.consume();
			}
		} finally {
			currentPoint = null;
			startPoint = null;
			marqueeBounds = null;
			previousCursor = null;
		}
	}

	/**
	  * Includes the specified startPoint in the marquee selection. Calls
	  * overlay.
	  */
	public void mouseDragged(MouseEvent e) {
	
		if (!e.isConsumed() && startPoint != null) {
			if (!(e.getSource() instanceof JGraph))
				throw new IllegalArgumentException(
					"MarqueeHandler cannot handle event from unknown source: "
						+ e);
			JGraph graph = (JGraph) e.getSource();
			Graphics g = graph.getGraphics();
			Color bg = graph.getBackground();
			Color fg = graph.getMarqueeColor();
			g.setColor(fg);
			g.setXORMode(bg);
			overlay(g);
			currentPoint = e.getPoint();
			marqueeBounds = new Rectangle(startPoint);
			marqueeBounds.add(currentPoint);
			g.setColor(bg);
			g.setXORMode(fg);
			overlay(g);
			e.consume();
		}
	}

	/** 
		* Called after the component was repainted (ie. after autoscroll).
	  * This is used to indicate that the graphics is no more dirty.
	  */
	public void paint(Graphics g) {
		overlay(g);
	}

	protected void overlay(Graphics g) {
		if (marqueeBounds != null)
			g.drawRect(
				marqueeBounds.x,
				marqueeBounds.y,
				marqueeBounds.width,
				marqueeBounds.height);
	}

	/**
	  * Start the marquee at the specified startPoint. This invokes
	  * expandMarqueeToPoint to initialize marquee selection.
	  */
	public void mousePressed(MouseEvent e) {
	
		if (!e.isConsumed()) {
			if (!(e.getSource() instanceof JGraph))
				throw new IllegalArgumentException(
					"MarqueeHandler cannot handle event from unknown source: "
						+ e);
			JGraph graph = (JGraph) e.getSource();
			startPoint = e.getPoint();
			marqueeBounds = new Rectangle(startPoint);
			previousCursor = graph.getCursor();
			graph.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
			e.consume();
		}
	}

	/**
	 * Empty.
	 */
	public void mouseMoved(MouseEvent e) {
	}

	/**
	 * Returns the currentPoint.
	 * @return Point
	 */
	public Point getCurrentPoint() {
		return currentPoint;
	}

	/**
	 * Returns the marqueeBounds.
	 * @return Rectangle
	 */
	public Rectangle getMarqueeBounds() {
		return marqueeBounds;
	}

	/**
	 * Returns the previousCursor.
	 * @return Cursor
	 */
	public Cursor getPreviousCursor() {
		return previousCursor;
	}

	/**
	 * Returns the startPoint.
	 * @return Point
	 */
	public Point getStartPoint() {
		return startPoint;
	}

	/**
	 * Sets the currentPoint.
	 * @param currentPoint The currentPoint to set
	 */
	public void setCurrentPoint(Point currentPoint) {
		this.currentPoint = currentPoint;
	}

	/**
	 * Sets the marqueeBounds.
	 * @param marqueeBounds The marqueeBounds to set
	 */
	public void setMarqueeBounds(Rectangle marqueeBounds) {
		this.marqueeBounds = marqueeBounds;
	}

	/**
	 * Sets the previousCursor.
	 * @param previousCursor The previousCursor to set
	 */
	public void setPreviousCursor(Cursor previousCursor) {
		this.previousCursor = previousCursor;
	}

	/**
	 * Sets the startPoint.
	 * @param startPoint The startPoint to set
	 */
	public void setStartPoint(Point startPoint) {
		this.startPoint = startPoint;
	}

}