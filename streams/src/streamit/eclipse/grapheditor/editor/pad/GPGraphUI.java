/*
 * @(#)GPGraphUI.java	1.2 11/11/02
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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.Reader;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.jgraph.JGraph;
import org.jgraph.graph.CellView;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphTransferable;
import org.jgraph.graph.ParentMap;
import org.jgraph.plaf.basic.BasicGraphUI;

public class GPGraphUI extends BasicGraphUI {

	public GPGraph getGPGraph() {
		return (GPGraph) graph;
	}

	//
	// Override Parent Methods
	//
	// @jdk14
	protected TransferHandler createTransferHandler() {
		return new GPTransferHandler();
	}

	/**
	 * Paint the background of this graph. Calls paintGrid.
	 */
	protected void paintBackground(Graphics g) {
		Rectangle pageBounds = graph.getBounds();
		if (getGPGraph().getBackgroundImage() != null) {
			// Use clip and pageBounds
			double s = graph.getScale();
			Graphics2D g2 = (Graphics2D) g;
			AffineTransform tmp = g2.getTransform();
			g2.scale(s, s);
			g.drawImage(getGPGraph().getBackgroundImage(), 0, 0, graph);
			g2.setTransform(tmp);
		} else if (getGPGraph().isPageVisible()) { // FIX: Use clip
			int w = (int) (getGPGraph().getPageFormat().getWidth());
			int h = (int) (getGPGraph().getPageFormat().getHeight());
			Point p = graph.toScreen(new Point(w, h));
			w = p.x;
			h = p.y;
			g.setColor(graph.getHandleColor());
			g.fillRect(0, 0, graph.getWidth(), graph.getHeight());
			g.setColor(Color.darkGray);
			g.fillRect(3, 3, w, h);
			g.setColor(graph.getBackground());
			g.fillRect(1, 1, w - 1, h - 1);
			pageBounds = new Rectangle(0, 0, w, h);
		}
		if (graph.isGridVisible())
			paintGrid(graph.getGridSize(), g, pageBounds);
	}

	/**
	 * TransferHandler that can import text.
	 */
	public class GPTransferHandler extends GraphTransferHandler {

		protected GraphTransferable create(
			JGraph graph,
			Object[] cells,
			Map viewAttributes,
			Rectangle bounds,
			ConnectionSet cs,
			ParentMap pm) {
			String gxl = GPConverter.toGXL(getGPGraph(), cells);
			return new GPTransferable(gxl, cells, viewAttributes, bounds, cs, pm);
		}

		public boolean importDataImpl(JComponent comp, Transferable t) {
			if (super.importDataImpl(comp, t)) {
				return true;
			} else if (graph.isDropEnabled() && comp instanceof JGraph) {
				try {
					JGraph graph = (JGraph) comp;
					// Drop Files from OS
					if (t
						.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
						Point p = null;
						if (getInsertionLocation() != null)
							p =
								graph.fromScreen(
									graph.snap(
										new Point(getInsertionLocation())));
						int gs = graph.getGridSize();
						if (p == null)
							p = new Point(gs, gs);
						Map attributes = new Hashtable();
						java.util.List cells = new LinkedList();
						java.util.List fileList =
							(java.util.List) t.getTransferData(
								DataFlavor.javaFileListFlavor);
						Iterator iterator = fileList.iterator();
						while (iterator.hasNext()) {
							File file = (File) iterator.next();

							Hashtable hashtable = new Hashtable();
							hashtable.put("name", file.getName());
							//hashtable.put(
							//	"url",
							//	"file://" + file.getAbsolutePath());
							hashtable.put(GPUserObject.keyURI, file.toURL().toString());
							hashtable.put("path", file.getAbsolutePath());

							// Try icon 																					// 1.3, 1.4 use imageIO
							URL url = file.toURL();
							ImageIcon icon = null;
							String name = file.getName();
							if (url.toString().toLowerCase().endsWith(".gif")
								|| url.toString().toLowerCase().endsWith(".jpg")
								|| url.toString().toLowerCase().endsWith(".jpeg")
								|| url.toString().toLowerCase().endsWith(".png")) {
								icon = new ImageIconBean(url);
								name = "";
							}

							Object userObject = new GPUserObject(name, hashtable);
							DefaultGraphCell cell;
							
							if (icon != null)
								cell = new ImageCell(userObject);
							else
								cell = new DefaultGraphCell(userObject);
							cell.add(new DefaultPort());

							Dimension d;
							if (icon == null) {
								CellView view =
									graphLayoutCache.getMapping(cell, true);
								d = graph.snap(getPreferredSize(graph, view));
								if (d == null)
									d = new Dimension(2 * gs, 2 * gs);
								else
									d.width = d.width + 10;
								graph.snap(d);
								d.width += 1;
								d.height += 1;
							} else
								d =
									new Dimension(
										icon.getIconWidth(),
										icon.getIconHeight());

							Map map = GraphConstants.createMap();
							GraphConstants.setBounds(map, new Rectangle(p, d));
							if (icon != null)
								GraphConstants.setIcon(map, icon);
							else
								GraphConstants.setBorderColor(map, Color.black);
							attributes.put(cell, map);
							cells.add(cell);
							p.translate(0, d.height + (int) (1.5 * gs));
							graph.snap(p);
						}
						if (!cells.isEmpty()) {
							graph.getGraphLayoutCache().insert(
								cells.toArray(),
								attributes,
								null,
								null,
								null);
							graph.requestFocus();
							return true;
						}

					// Try to drop as text
					} else {
						Object cell = null;
						Map map = GraphConstants.createMap();
						DataFlavor bestFlavor =
							DataFlavor.selectBestTextFlavor(
								t.getTransferDataFlavors());
						if (bestFlavor != null && cell == null) {
							Reader reader = bestFlavor.getReaderForText(t);
							StringBuffer s = new StringBuffer();
							char[] c = new char[1];
							while (reader.read(c) != -1)
								s.append(c);
							Point p =
								graph.fromScreen(
									graph.snap(
										new Point(getInsertionLocation())));
							int gs = graph.getGridSize();
							if (p == null)
								p = new Point(gs, gs);
							cell = new TextCell(new String(s));
							((DefaultGraphCell) cell).add(new DefaultPort());
							CellView view =
								graphLayoutCache.getMapping(cell, true);
							Dimension d =
								graph.snap(getPreferredSize(graph, view));
							if (d == null)
								d = new Dimension(2 * gs, 2 * gs);
							GraphConstants.setBounds(map, new Rectangle(p, d));
						}
						if (cell != null) {
							Map viewMap = new Hashtable();
							viewMap.put(cell, map);
							graph.getModel().insert(
								new Object[] { cell },
								viewMap,
								null,
								null,
								null);
						}
					}
				} catch (Exception exception) {
					//System.out.println(
					//	"Cannot import data: " + exception.getMessage());
				}
			}
			return false;
		}

	}

}
