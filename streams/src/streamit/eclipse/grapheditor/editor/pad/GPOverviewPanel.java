/*
 * @(#)GPOverview.java	1.2 11/11/02
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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.border.MatteBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jgraph.JGraph;
import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;
import org.jgraph.graph.CellView;
import org.jgraph.graph.GraphLayoutCache;

public class GPOverviewPanel
	extends JPanel
	implements ComponentListener, GraphModelListener, Observer {
	protected GPGraph graph;
	protected JGraph originalGraph;
	protected PannerViewfinder v;
	protected Rectangle r;
	double graphWindowToPannerScale = 0.5;
	static final int PANEL_BUFFER = 2;


	/** GPOverviewPanel provides a 'birds-eye' view of the active document. 
	 * Must use the {@link #createOverviewPanel} method to create a new instance.
	 */
	protected GPOverviewPanel(JGraph g, GPDocument d) {
		originalGraph = g;
		v = new PannerViewfinder(this, d.getScrollPane().getViewport());
		GraphLayoutCache view = new ViewRedirector(g, g.getGraphLayoutCache());
		graph = new GPGraph(g.getModel(), view);
		graph.setAntiAliased(true);
		graph.getModel().addGraphModelListener(this);
		graph.setEnabled(false);
		graph.addMouseListener(v);
		graph.addMouseMotionListener(v);

		g.addPropertyChangeListener(JGraph.SCALE_PROPERTY, v);

		addComponentListener(this);
		g.getGraphLayoutCache().addObserver(this);
		setLayout(new BorderLayout());
		add(graph, BorderLayout.CENTER);
	}

	/** GPOverviewPanel factory that returns instance with small inset as a buffer.*/
	public static JPanel createOverviewPanel(JGraph g, GPDocument d){
		JPanel panelWithInternalOffset = new JPanel();
		panelWithInternalOffset.setLayout(new BorderLayout());
		panelWithInternalOffset.setBorder(new MatteBorder(PANEL_BUFFER,PANEL_BUFFER,PANEL_BUFFER,PANEL_BUFFER,Color.WHITE));
		panelWithInternalOffset.add(new GPOverviewPanel(g,d), BorderLayout.CENTER);
		return panelWithInternalOffset;
	}

	//
	// Observer
	//
	public void update(Observable o, Object arg) {
		componentResized(null);
	}

	public void paintChildren(Graphics g) {
		super.paintChildren(g);
		v.update(g);
	}

	//
	// GraphModelListener
	//

	public void graphChanged(GraphModelEvent e) {
		componentResized(null);
	}

	//
	// Component Listener
	//

	public void componentResized(ComponentEvent e) {

		//when user resizes panner, original window view's size is changed according
		//to zoom factor - hence we have to take it into account

		Dimension d = v.getViewport().getView().getSize();
		d.setSize(d.width * 1 / v.zoomScale, d.height * 1 / v.zoomScale);
		Dimension s = getSize();
		double sx = s.getWidth() / d.getWidth();
		double sy = s.getHeight() / d.getHeight();
		graphWindowToPannerScale = Math.min(sx, sy);
		graph.setScale(graphWindowToPannerScale);
		v.synchViewportWithPanner();
		repaint();
	}

	public void componentShown(ComponentEvent e) {
		componentResized(e);
	}

	public void componentHidden(ComponentEvent e) {
	}

	public void componentMoved(ComponentEvent e) {
	}

	//
	// View Redirector
	//

	public class ViewRedirector extends GraphLayoutCache {

		protected GraphLayoutCache realView;

		public ViewRedirector(JGraph graph, GraphLayoutCache realView) {
			super(null, graph);
			this.realView = realView;
			setModel(graph.getModel());

		}

		public CellView[] getRoots() {
			return realView.getRoots();
		}

		public CellView getMapping(Object cell, boolean create) {
			if (realView != null)
				return realView.getMapping(cell, create);
			return null;
		}

		public void putMapping(Object cell, CellView view) {
			if (realView != null)
				realView.putMapping(cell, view);
		}

	}

	class PannerViewfinder
		implements MouseListener, MouseMotionListener, PropertyChangeListener, ChangeListener {
		public final int LEFT_VERTICAL = 1;
		public final int RIGHT_VERTICAL = 2;
		public final int UPPER_HORIZONTAL = 4;
		public final int LOWER_HORIZONTAL = 8;
		public final int NW_CORNER = 5;
		public final int NE_CORNER = 6;
		public final int SW_CORNER = 9;
		public final int SE_CORNER = 10;

		int scaledWidth = 50;
		int scaledHeight = 25;

		int last_x, last_y;

		Rectangle pannerContainerRectangle;
		Rectangle pannerViewRectangle;
		Rectangle pannerViewRectangleCopy;
		Rectangle pannerContainerRectangleCopy;

		Rectangle pannerResizeDecoration;
		Container container;
		JViewport viewport;
		double zoomScale = 1.0;
		double combinedScale = graphWindowToPannerScale / zoomScale;

		boolean isActive = false;
		boolean isResizing = false;

		BasicStroke stroke = new BasicStroke(1.0f);
		float dash1[] = { 2.5f };
		BasicStroke dashStroke =
			new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash1, 0.0f);

		private Point viewportPosition;
		static final int DECORATION_SIZE = 4;

		public PannerViewfinder(Container c, JViewport jvp) {
			this.container = c;
			this.viewport = jvp;
			viewport.addChangeListener(this);
			pannerViewRectangle = new Rectangle(0,0, scaledWidth, scaledHeight);
			viewportPosition = new Point(0, 0);
			pannerResizeDecoration = new Rectangle(DECORATION_SIZE, DECORATION_SIZE);
		}

		private boolean isContained(MouseEvent me) {
			return pannerViewRectangle.contains(me.getX(), me.getY());
		}

		public JViewport getViewport() {
			return viewport;
		}

		//invoked when user scrolls viewport
		public void stateChanged(ChangeEvent e) {
			if (!isResizing) {
				synchViewportWithPanner();
				container.repaint();
			}
		}

		public void synchViewportWithPanner() {
			Point p = viewport.getViewPosition();
			combinedScale = graphWindowToPannerScale / zoomScale;
			int pannerx = (int) (p.getX() * combinedScale);
			int pannery = (int) (p.getY() * combinedScale);

			pannerViewRectangle.setLocation(pannerx, pannery);
		}

		//invoked when user changes zoom in graphpad
		public void propertyChange(PropertyChangeEvent evt) {
			if (!isResizing) {
				zoomScale = ((Double) evt.getNewValue()).doubleValue();
				combinedScale = graphWindowToPannerScale / zoomScale;
				container.repaint();
			}
		}

		public void mousePressed(MouseEvent e) {
			Cursor c = container.getCursor();
			int cursorType = c.getType();

			//regular actions (click recenter, drag panner around)
			if (cursorType == Cursor.DEFAULT_CURSOR) {
				last_x = pannerViewRectangle.x - e.getX();
				last_y = pannerViewRectangle.y - e.getY();
				if (isContained(e)) {
					updatePannerLocation(e);
					isActive = true;
				}
			}
			//resizing action
			else {
				isResizing = true;
				pannerViewRectangleCopy = new Rectangle(pannerViewRectangle);
				pannerContainerRectangleCopy = new Rectangle(pannerContainerRectangle);
			}

		}

		public void mouseDragged(MouseEvent e) {
			if (isActive) {
				updatePannerLocation(e);
				updateViewPort(e);
			}
			else if (isResizing) {
				Cursor c = container.getCursor();
				int cursorType = c.getType();

				switch (cursorType) {
					case Cursor.SE_RESIZE_CURSOR :
						SECornerResize(e);
						break;
					case Cursor.NE_RESIZE_CURSOR :
						//NECornerResize(e);
						break;
					case Cursor.SW_RESIZE_CURSOR :
						//SWCornerResize(e);
						break;
					case Cursor.NW_RESIZE_CURSOR :
						//NWCornerResize(e);
						break;
				}
			}
		}

		private void SECornerResize(MouseEvent e) {

			//resizing from SE corner
			int x = (int) pannerViewRectangleCopy.getX();
			int y = (int) pannerViewRectangleCopy.getY();
			double wx = e.getX() - pannerViewRectangleCopy.getMinX();
			double hx = e.getY() - pannerViewRectangleCopy.getMinY();
			if (wx > hx) {
				hx = wx * pannerViewRectangleCopy.getHeight() / pannerViewRectangleCopy.getWidth();
			}
			else {
				wx = hx * pannerViewRectangleCopy.getWidth() / pannerViewRectangleCopy.getHeight();
			}
			pannerViewRectangle.setBounds(x, y, (int) wx, (int) hx);
			ensureMinResize();
			ensureMaxResize();

			//recalculate scale
			double x_scale = (pannerViewRectangle.getWidth() / pannerViewRectangleCopy.getWidth());
			originalGraph.setScale((1 / (x_scale)) * zoomScale);

			container.repaint();
		}

		private void SWCornerResize(MouseEvent e) {
		}

		private void NWCornerResize(MouseEvent e) {
		}

		private void NECornerResize(MouseEvent e) {
		}

		private int findClosestDragPoint(MouseEvent e, double buffer) {

			double mx = e.getX();
			double my = e.getY();

			double lx = pannerViewRectangle.getX();
			double uy = pannerViewRectangle.getY();

			double rx = lx + pannerViewRectangle.getWidth();
			double ly = uy + pannerViewRectangle.getHeight();

			int mask = 0;
			//leftVertical
			if (Math.abs(mx - lx) < buffer && (my + buffer > uy && my < ly + buffer)) {
				//lv
				mask |= LEFT_VERTICAL;
			}
			if (Math.abs(mx - rx) < buffer && (my + buffer > uy && my < ly + buffer)) {
				//rv
				mask |= RIGHT_VERTICAL;
			}
			if (Math.abs(my - uy) < buffer && (mx + buffer > lx && mx < rx + buffer)) {
				//uh
				mask |= UPPER_HORIZONTAL;
			}
			if (Math.abs(my - ly) < buffer && (mx + buffer > lx && mx < rx + buffer)) {
				//lh
				mask |= LOWER_HORIZONTAL;
			}
			return mask;
		}

		public void mouseReleased(MouseEvent e) {
			if (isActive) {
				isActive = false;
			}
			if (isResizing) {
				zoomScale = originalGraph.getScale();
				combinedScale = graphWindowToPannerScale / zoomScale;
				isResizing = false;
				updateViewPort(e);
			}			
		}

		private void updateViewPort(MouseEvent e) {
			int x = (int) (1 / combinedScale * pannerViewRectangle.getX());
			int y = (int) (1 / combinedScale * pannerViewRectangle.getY());

			Rectangle r = viewport.getViewRect();

			//ensure that we can not go out of view bounds while setting viewport
			if (x + r.getWidth() > viewport.getViewSize().getWidth()) {
				x = (int) (viewport.getViewSize().getWidth() - r.getWidth());
			}
			if (y + r.getHeight() > viewport.getViewSize().getHeight()) {
				y = (int) (viewport.getViewSize().getHeight() - r.getHeight());
			}
			viewportPosition.setLocation(x, y);
			viewport.setViewPosition(viewportPosition);
		}

		public void mouseClicked(MouseEvent e) {
			//move panner viewport to new location
			if (!isContained(e)) {
				isActive = true;
				pannerViewRectangle.setLocation(e.getPoint());
				container.repaint();
				isActive = false;
				updateViewPort(e);
			}
		}

		public void mouseMoved(MouseEvent e) {
			//detect resizing points
			int dragPoint = findClosestDragPoint(e, 5.0);
			int cursor = convertDragPointToCursor(dragPoint);
			container.setCursor(Cursor.getPredefinedCursor(cursor));
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		private int convertDragPointToCursor(int dragPoint) {

			//for now, we are only interested in se corner
			int cursor = Cursor.DEFAULT_CURSOR;
			switch (dragPoint) {
				case UPPER_HORIZONTAL :
				case LOWER_HORIZONTAL :
				case LEFT_VERTICAL :
				case RIGHT_VERTICAL :
				case SW_CORNER :
				case NW_CORNER :
				case NE_CORNER :
					cursor = Cursor.DEFAULT_CURSOR;
					break;
				case SE_CORNER :
					cursor = Cursor.SE_RESIZE_CURSOR;
					break;
			}
			return cursor;
		}

		public void updatePannerLocation(MouseEvent e) {
			pannerViewRectangle.setLocation(last_x + e.getX(), last_y + e.getY());
			ensureWithinPannerBounds();
			container.repaint();
		}

		public void update(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;

			Rectangle viewportR = viewport.getViewRect();
			Dimension viewD = viewport.getView().getSize();
			combinedScale = graphWindowToPannerScale / zoomScale;

			if (pannerContainerRectangle == null) {
				pannerContainerRectangle = new Rectangle();
			}
			pannerContainerRectangle.setSize(
				((int) (viewD.getWidth() * combinedScale)) - PANEL_BUFFER,
				((int) (viewD.getHeight() * combinedScale)) - PANEL_BUFFER);

			scaledWidth =
				(int) (viewportR.getWidth()
					* pannerContainerRectangle.getWidth()
					/ viewD.getWidth());
			scaledHeight =
				(int) (viewportR.getHeight()
					* pannerContainerRectangle.getHeight()
					/ viewD.getHeight());

			if (!isResizing) {
				g2.setStroke(stroke);
				pannerViewRectangle.setSize(scaledWidth, scaledHeight);
			}
			else {
				g2.setStroke(dashStroke);
			}

			int maxx = (int) pannerViewRectangle.getMaxX();
			int maxy = (int) pannerViewRectangle.getMaxY();
			pannerResizeDecoration.setLocation(maxx - DECORATION_SIZE/2,  
											   maxy - DECORATION_SIZE/2);

			g2.setColor(Color.red);
			g2.fill(pannerResizeDecoration);
			g2.draw(pannerViewRectangle);
		}

		private void ensureWithinPannerBounds() {
			int new_x = pannerViewRectangle.x;
			int new_y = pannerViewRectangle.y;

			if ((pannerViewRectangle.x + scaledWidth) > pannerContainerRectangle.getWidth()) {
				new_x = (int) pannerContainerRectangle.getWidth() - scaledWidth;
			}

			if (pannerViewRectangle.x < 0) {
				new_x = 1;
			}
			if ((pannerViewRectangle.y + scaledHeight) > pannerContainerRectangle.getHeight()) {
				new_y = (int) pannerContainerRectangle.getHeight() - scaledHeight;
			}
			if (pannerViewRectangle.y < 0) {
				new_y = 1;
			}
			pannerViewRectangle.setLocation(new_x, new_y);
		}

		private void ensureMaxResize() {
			//ensure resizing doesn't go out of the bounds of
			//pannerContainer, if hit is detected prevent further
			//resizing

			if (!pannerContainerRectangle.contains(pannerViewRectangle)) {
				Rectangle r = pannerViewRectangle.intersection(pannerContainerRectangleCopy);
				if (r.width < pannerViewRectangle.width) {
					pannerViewRectangle.width = r.width;
					pannerViewRectangle.height =
						pannerViewRectangle.width
							* pannerViewRectangleCopy.height
							/ pannerViewRectangleCopy.width;
				}
				if (r.height < pannerViewRectangle.height) {
					pannerViewRectangle.height = r.height;
					pannerViewRectangle.width =
						pannerViewRectangle.height
							* pannerViewRectangleCopy.width
							/ pannerViewRectangleCopy.height;
				}
			}
		}

		private void ensureMinResize() {
			int minSize = 20;
			Rectangle pvrc = pannerViewRectangleCopy;
			if (pvrc.width > pvrc.height && pannerViewRectangle.width < minSize) {
				pannerViewRectangle.width = minSize;
				pannerViewRectangle.height = (int) (pvrc.getHeight() * minSize / pvrc.getWidth());
			}
			else if (pvrc.height > pvrc.width && pannerViewRectangle.height < minSize) {
				pannerViewRectangle.height = minSize;
				pannerViewRectangle.width = (int) (pvrc.getHeight() * minSize / pvrc.getWidth());
			}
		}
	}
}