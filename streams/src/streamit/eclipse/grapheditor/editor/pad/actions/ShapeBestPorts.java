/*
 * @(#)ShapeBestPorts.java	1.2 30.01.2003
 *
 * Copyright (C) 2003 sven.luzar
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.Point;
import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.Port;
import org.jgraph.graph.PortView;
import streamit.eclipse.grapheditor.editor.pad.GPGraph;

/**
 * Action that sets the selections fill color using a color dialog.
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class ShapeBestPorts extends AbstractActionDefault {

	/**
	 * Constructor for ShapeBestPorts.
	 * @param graphpad
	 * @param name
	 */
	public ShapeBestPorts(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */

		public void actionPerformed(ActionEvent e) {
			GPGraph graph = getCurrentGraph();
			Object[] all = graph.getDescendants(graph.getSelectionCells());
			Object[] edges = graph.getEdges(all);
			if (edges != null && edges.length > 0) {
				ConnectionSet cs = new ConnectionSet();
				for (int i = 0; i < edges.length; i++) {
					EdgeView view =
						(EdgeView) graphpad.getCurrentDocument().getGraphLayoutCache().getMapping(edges[i], false);
					if (view != null) {
						Object orig = graph.getModel().getSource(edges[i]);
						if (orig != null) {
							Point to = view.getPoint(1);
							Object source = graph.getSourceVertex(edges[i]);
							Port port = findClosestPort(to, source);
							if (port != orig)
								cs.connect(edges[i], port, true);
						}
						orig = graph.getModel().getTarget(edges[i]);
						if (orig != null) {
							Point to = view.getPoint(view.getPointCount() - 1);
							Object target = graph.getTargetVertex(edges[i]);
							Port port = findClosestPort(to, target);
							if (port != orig)
								cs.connect(edges[i], port, false);
						}
					}
				} // end for buttonImage
				graph.getModel().edit(null, cs, null, null);
			}
		}
		
	/* Return the port of the given vertex that is closest to the given point. */
	public Port findClosestPort(Point p, Object vertex) {
		GPGraph graph = getCurrentGraph();
		Port port = null;
		double min = Double.MAX_VALUE;
		for (int i = 0; i < graph.getModel().getChildCount(vertex); i++) {
			Object child = graph.getModel().getChild(vertex, i);
			if (child instanceof Port) {
				PortView view = (PortView) getCurrentGraphLayoutCache().getMapping(child, false);
				double t = p.distance(view.getLocation(null));
				if (port == null || t < min) {
					port = (Port) child;
					min = t;
				}
			}
		}
		return port;
	}
		
}
