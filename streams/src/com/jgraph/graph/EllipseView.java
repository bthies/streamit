/*
 * Created on Jul 17, 2003
 *
 * 
 */
package com.jgraph.graph;

/**
 * @author jcarlos
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import com.jgraph.JGraph;

public class EllipseView extends VertexView {

	public static EllipseRenderer renderer = new EllipseRenderer();

	public EllipseView(Object cell, JGraph graph, CellMapper cm) {
		super(cell, graph, cm);
	}

	/**
	 * Returns the intersection of the bounding rectangle and the
	 * straight line between the source and the specified point p.
	 * The specified point is expected not to intersect the bounds.
	 */
	public Point getPerimeterPoint(Point source, Point p) {
		// Compute relative bounds
		Rectangle r = getBounds();
		int x = r.x;
		int y = r.y;
		int a = (r.width + 1) / 2;
		int b = (r.height + 1) / 2;

		// Get center
		int xCenter = (int) (x + a);
		int yCenter = (int) (y + b);

		// Compute angle
		int dx = p.x - xCenter;
		int dy = p.y - yCenter;
		double t = Math.atan2(dy, dx);

		// Compute Perimeter Point
		int xout = xCenter + (int) (a * Math.cos(t)) - 1;
		int yout = yCenter + (int) (b * Math.sin(t)) - 1;

		// Return perimeter point
		return new Point(xout, yout);
	}

	public CellViewRenderer getRenderer() {
		return renderer;
	}

	public static class EllipseRenderer extends VertexRenderer {

		public void paint(Graphics g) {
			int b = borderWidth;
			Graphics2D g2 = (Graphics2D) g;
			Dimension d = getSize();
			boolean tmp = selected;
			if (super.isOpaque()) {
				g.setColor(super.getBackground());
				g.fillOval(b - 1, b - 1, d.width - b, d.height - b);
			}
			try {
				setBorder(null);
				setOpaque(false);
				selected = false;
				super.paint(g);
			} finally {
				selected = tmp;
			}
			if (bordercolor != null) {
				g.setColor(bordercolor);
				g2.setStroke(new BasicStroke(b));
				g.drawOval(b - 1, b - 1, d.width - b, d.height - b);
			}
			if (selected) {
				g2.setStroke(GraphConstants.SELECTION_STROKE);
				g.setColor(graph.getHighlightColor());
				g.drawOval(b - 1, b - 1, d.width - b, d.height - b);
			}
		}
	}

}