/*
 * @(#)RealGraphCellRenderer.java	1.2 11/11/02
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

package streamit.eclipse.grapheditor.editor.pad;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;

import org.jgraph.graph.AbstractCellView;
import org.jgraph.graph.CellView;

public class RealGraphCellRenderer extends JComponent {

	protected CellRendererPane rendererPane;
	protected CellView[] views;
	protected GPGraph graph;
	protected double scale = 1.0;

	public RealGraphCellRenderer(GPGraph graph, CellView[] views) {
		add(rendererPane = new CellRendererPane());
		this.views = views;
		this.graph = graph;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}

	public Dimension getPreferredSize() {
		if (views != null) {
			Rectangle r = AbstractCellView.getBounds(views);
			r.width *= scale;
			r.height *= scale;
			return new Dimension(r.width + 2, r.height + 2);
		}
		return new Dimension(10, 10);
	}

	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		AffineTransform at = g2.getTransform();
		g2.scale(scale, scale);
		if (views != null) {
			Rectangle r = AbstractCellView.getBounds(views);
			g.translate(-r.x, -r.y);
			for (int i = 0; i < views.length; i++) {
				Rectangle b = views[i].getBounds();
				Component c;
				if (graph.isGroup(views[i].getCell()))
					c =
						new RealGraphCellRenderer(
							graph,
							((CellView) views[i]).getChildViews());
				else
					c =
						views[i].getRendererComponent(
							graph,
							false,
							false,
							false);
				rendererPane.paintComponent(
					g,
					c,
					this,
					b.x,
					b.y,
					b.width,
					b.height);
			}
		}
		g2.setTransform(at);
	}

}