/*
 * @(#)Rule.java	1.2 11/11/02
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.JComponent;

import org.jgraph.JGraph;

public class Rule
	extends JComponent
	implements MouseMotionListener, PropertyChangeListener {
	public static final Color middleGray = new Color(170, 170, 170);
	public static final NumberFormat nf = NumberFormat.getInstance();
	public static final int INCH = 72;
	public static final int HORIZONTAL = 0;
	public static final int VERTICAL = 1;
	public static final int SIZE = 15;

	public int activeoffset, activelength;

	private transient JGraph graph;
	public int orientation;
	public boolean isMetric;
	private double increment;
	private double units;
	private Point drag, mouse = new Point();

	public Rule(int o, boolean m, JGraph graph) {
		this.graph = graph;
		orientation = o;
		isMetric = m;
		setIncrementAndUnits();
		nf.setMaximumFractionDigits(2);
		graph.addMouseMotionListener(this);
		graph.addPropertyChangeListener(this);
	}

	public void setIsMetric(boolean isMetric) {
		this.isMetric = isMetric;
		setIncrementAndUnits();
		repaint();
	}

	private void setIncrementAndUnits() {
		if (isMetric) {
			units = INCH / 2.54; // 2.54 dots per centimeter
			units *= graph.getScale();
			increment = units;
		} else {
			units = INCH;
			units *= graph.getScale();
			increment = units / 2;
		}
	}

	public boolean isMetric() {
		return this.isMetric;
	}

	public double getIncrement() {
		return increment;
	}

	public void setActiveOffset(int offset) {
		activeoffset = offset;
	}

	public void setActiveLength(int length) {
		activelength = length;
	}

	public Dimension getPreferredSize() {
		Dimension dim = graph.getPreferredSize();
		if (orientation == HORIZONTAL)
			dim.height = SIZE;
		else
			dim.width = SIZE;
		return dim;
	}

	// from MouseMotionListener
	public void mouseMoved(MouseEvent e) {
		if (drag != null) {
			Point old = drag;
			drag = null;
			repaintStripe(old.x, old.y);
		}
		Point old = mouse;
		mouse = e.getPoint();
		repaintStripe(old.x, old.y);
		repaintStripe(mouse.x, mouse.y);
	}

	// from MouseMotionListener
	public void mouseDragged(MouseEvent e) {
		Point old = drag;
		drag = e.getPoint();
		if (old != null)
			repaintStripe(old.x, old.y);
		repaintStripe(drag.x, drag.y);
	}

	// from PropertyChangeListener
	public void propertyChange(PropertyChangeEvent event) {
		String changeName = event.getPropertyName();
		if (changeName.equals(JGraph.SCALE_PROPERTY))
			repaint();
	}

	public void repaintStripe(int x, int y) {
		if (orientation == HORIZONTAL)
			repaint(x, 0, 1, SIZE);
		else
			repaint(0, y, SIZE, 1);
	}

	public void paintComponent(Graphics g) {
		revalidate();
		setIncrementAndUnits();
		Rectangle drawHere = g.getClipBounds();

		// Fill clipping area with graph parent background.
		if (activelength > 0)
			g.setColor(middleGray);
		else
			g.setColor(Color.lightGray);
		g.fillRect(drawHere.x, drawHere.y, drawHere.width, drawHere.height);
		Point p = graph.toScreen(new Point(activeoffset, activelength));
		//p.x = (int) (p.x * Rule.INCH/72);
		//p.y = (int) (p.y * Rule.INCH/72);

		// draw "active region"
		g.setColor(Color.lightGray);
		if (orientation == HORIZONTAL)
			g.fillRect(p.x, drawHere.y, p.y, drawHere.height);
		else
			g.fillRect(drawHere.x, p.x, drawHere.width, p.y);

		// Do the ruler labels in a small font that'buttonSelect black.
		g.setFont(new Font("SansSerif", Font.PLAIN, 8));
		g.setColor(Color.black);

		// Some vars we need.
		double end = 0;
		double start = 0;
		int tickLength = 0;
		String text = null;

		// Use clipping bounds to calculate first tick and last tick location.
		if (orientation == HORIZONTAL) {
			start = Math.floor(drawHere.x / increment) * increment;
			end =
				Math.ceil((drawHere.x + drawHere.width) / increment)
					* increment;
		} else {
			start = Math.floor(drawHere.y / increment) * increment;
			end =
				Math.ceil((drawHere.y + drawHere.height) / increment)
					* increment;
		}

		// Make a special case of 0 to display the number
		// within the rule and draw a units label.
		if (start == 0) {
			text = Integer.toString(0) + (isMetric ? " cm" : " in");
			tickLength = 10;
			if (orientation == HORIZONTAL) {
				g.drawLine(0, SIZE - 1, 0, SIZE - tickLength - 1);
				g.drawString(text, 2, 11);
			} else {
				g.drawLine(SIZE - 1, 0, SIZE - tickLength - 1, 0);
				g.drawString(text, 1, 11);
			}
			text = null;
			start = increment;
		}

		// ticks and labels
		boolean label = false;
		for (double i = start; i < end; i += increment) {
			if (units == 0)
				units = 1;
			tickLength = 10;
//VW make relative to scaling factor
			text = nf.format(i / units);
			label = false;

			if (tickLength != 0) {
				if (orientation == HORIZONTAL) {
					g.drawLine(
						(int) i,
						SIZE - 1,
						(int) i,
						SIZE - tickLength - 1);
					if (text != null)
						g.drawString(text, (int) i + 2, 11);
				} else {
					g.drawLine(
						SIZE - 1,
						(int) i,
						SIZE - tickLength - 1,
						(int) i);
					if (text != null)
						g.drawString(text, 0, (int) i + 9);
				}
			}
		}

		// Draw Mouseposition
		//if (graph.hasFocus()) {
		g.setColor(Color.green);
		if (orientation == HORIZONTAL)
			g.drawLine(mouse.x, SIZE - 1, mouse.x, SIZE - tickLength - 1);
		else
			g.drawLine(SIZE - 1, mouse.y, SIZE - tickLength - 1, mouse.y);
		if (drag != null)
			if (orientation == HORIZONTAL)
				g.drawLine(drag.x, SIZE - 1, drag.x, SIZE - tickLength - 1);
			else
				g.drawLine(SIZE - 1, drag.y, SIZE - tickLength - 1, drag.y);
		//}
	}
}
