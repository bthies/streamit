/*
 * @(#)DefaultEdge.java	1.0 1/1/02
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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A simple implementation for an edge.
 *
 * @version 1.0 1/1/02
 * @author Gaudenz Alder
 */

public class DefaultEdge extends DefaultGraphCell implements Edge {

	protected static final int center = GraphConstants.PERCENT / 2;

	protected static final Point defaultLabel = new Point(center, center);

	public static final ArrayList defaultPoints = new ArrayList();

	static {
		defaultPoints.add(new Point(10, 10));
		defaultPoints.add(new Point(20, 20));
	}

	/** Source and target of the edge. */
	protected Object source, target;

	/**
	 * Constructs an empty edge.
	 */
	public DefaultEdge() {
		this(null);
	}

	/**
	 * Constructs an edge that holds a reference to the specified user object.
	 *
	 * @param userObject reference to the user object
	 */
	public DefaultEdge(Object userObject) {
		this(userObject, false);
	}

	/**
	 * Constructs an edge that holds a reference to the specified user object
	 * and sets default values for points and the label position.
	 *
	 * @param userObject reference to the user object
	 */
	public DefaultEdge(Object userObject, boolean allowsChildren) {
		super(userObject, allowsChildren);
		GraphConstants.setPoints(attributes, new ArrayList(defaultPoints));
		GraphConstants.setLabelPosition(attributes, defaultLabel);
	}

	/**
	 * Override parent method to ensure non-null points.
	 */
	public Map changeAttributes(Map change) {
		Map undo = super.changeAttributes(change);
		List points = GraphConstants.getPoints(attributes);
		if (points == null)
			GraphConstants.setPoints(attributes, new ArrayList(defaultPoints));
		return undo;
	}

	/**
	 * Returns the source of the edge.
	 */
	public Object getSource() {
		return source;
	}

	/**
	 * Returns the target of the edge.
	 */
	public Object getTarget() {
		return target;
	}

	/**
	 * Sets the source of the edge.
	 */
	public void setSource(Object port) {
		source = port;
	}

	/**
	 * Returns the target of <code>edge</code>.
	 */
	public void setTarget(Object port) {
		target = port;
	}

	/**
	 * Create a clone of the cell. The cloning of the
	 * user object is deferred to the cloneUserObject()
	 * method.
	 *
	 * @return Object  a clone of this object.
	 */
	public Object clone() {
		DefaultEdge c = (DefaultEdge) super.clone();
		c.source = null;
		c.target = null;
		return c;
	}

	//
	// Default Routing
	// 

	public static class DefaultRouting implements Edge.Routing {

		public void route(EdgeView edge, java.util.List points) {
			int n = points.size();
			Point from = edge.getPoint(0);
			if (edge.getSource() instanceof PortView)
				from = ((PortView) edge.getSource()).getLocation(null);
			else if (edge.getSource() != null)
				from = edge.getSource().getBounds().getLocation();
			Point to = edge.getPoint(n - 1);
			if (edge.getTarget() instanceof PortView)
				to = ((PortView) edge.getTarget()).getLocation(null);
			else if (edge.getTarget() != null)
				to = edge.getTarget().getBounds().getLocation();
			if (from != null && to != null) {
				Point[] routed;
				// Handle self references
				if (edge.getSource() == edge.getTarget()
					&& edge.getSource() != null) {
					Rectangle bounds =
						edge.getSource().getParentView().getBounds();
					int height = edge.getGraph().getGridSize();
					int width = (int) (bounds.getWidth() / 3);
					routed = new Point[4];
					routed[0] =
						new Point(
							bounds.x + width,
							bounds.y + bounds.height);
					routed[1] =
						new Point(
							bounds.x + width,
							bounds.y + bounds.height + height);
					routed[2] =
						new Point(
							bounds.x + 2 * width,
							bounds.y + bounds.height + height);
					routed[3] =
						new Point(
							bounds.x + 2 * width,
							bounds.y + bounds.height);
				} else {
					int dx = Math.abs(from.x - to.x);
					int dy = Math.abs(from.y - to.y);
					int x2 = from.x + ((to.x - from.x) / 2);
					int y2 = from.y + ((to.y - from.y) / 2);
					routed = new Point[2];
					if (dx > dy) {
						routed[0] = new Point(x2, from.y);
						//new Point(to.x, from.y)
						routed[1] = new Point(x2, to.y);
					} else {
						routed[0] = new Point(from.x, y2);
						// new Point(from.x, to.y)
						routed[1] = new Point(to.x, y2);
					}
				}
				// Set/Add Points
				for (int i=0; i<routed.length; i++)
					if (points.size() > i+2)
						points.set(i+1, routed[i]);
					else
						points.add(i+1, routed[i]);
				// Remove spare points
				while (points.size() > routed.length+2) {
					points.remove(points.size()-2);
				}
			}
		}

	}

}