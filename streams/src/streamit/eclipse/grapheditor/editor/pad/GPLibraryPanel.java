/*
 * @(#)GPLibraryPanel.java	1.2 11/11/02
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.Border;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import org.jgraph.graph.AbstractCellView;
import org.jgraph.graph.CellView;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphTransferable;
import org.jgraph.graph.ParentMap;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
//@jdk1.4 removed
//import org.jgraph.plaf.basic.TransferHandler;

//@jdk1.4 removed
//public class GPLibraryPanel extends TransferHandler.JAdapterComponent {
//@jdk1.4 added
public class GPLibraryPanel extends JComponent {

	public static final String LIBRARY_FORMAT = "LIB-1.0";

	/* Maximum size of library entries */
	protected int MAX = 60;

	protected ScrollablePanel panel;
	protected GraphCellsComponent selected = null;
	protected Border oldBorder = null;
	protected boolean dragging = false;
	protected JComboBox combo = new JComboBox();

	public boolean fullLibraryAccess = true;

	public GPLibraryPanel(String name, String[] files, int entrySize) {
		MAX = entrySize;
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(2 * MAX + 35, 0));
		// Don't forget to uncomment for the Java 1.3 release

		setTransferHandler(new DNDTransferHandler());
		add(combo, BorderLayout.NORTH);
		addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				setSelected(selected);
			}

			public void focusLost(FocusEvent e) {
				setSelected(selected);
			}

		});
		// Combo Box Handler (Library Switching)
		combo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object item = combo.getSelectedItem();
				if (item instanceof ScrollablePanel) {
					if (panel != null) {
						remove(panel.getParent().getParent());
						panel.getParent().getParent().remove(panel);
					}
					panel = (ScrollablePanel) item;
					JScrollPane scrollPane = new JScrollPane(panel);
					scrollPane.getViewport().setBackground(Color.white);
					add(scrollPane, BorderLayout.CENTER);
					// Add to each Lib!
					panel.getParent().addMouseListener(new MouseAdapter() {
						public void mousePressed(MouseEvent e) {
							setSelected(null);
							requestFocus();
						}
						public void mouseReleased(MouseEvent e) {
							if (isPopupTrigger(e))
								showPopupMenu(panel, e.getPoint());
						}
					});
					validate();
				}
			}
		});
		if (files != null && files.length > 0) {
			for (int i = 0; i < files.length; i++)
				openLibrary(files[i]);
		}
		if (files == null && name == null)
			name = "No Name";
		if (name != null && name.length() > 0)
			addLibrary(name);

		fullLibraryAccess =
			new Boolean(Translator.getString("FullLibraryAccess"))
				.booleanValue();

	}

	protected boolean isPopupTrigger(MouseEvent e) {
		return SwingUtilities.isRightMouseButton(e);
	}

	public void showPopupMenu(Component c, Point p) {
		Container parent = this;
		do {
			parent = parent.getParent();
		} while (parent != null && !(parent instanceof GPGraphpad));
		GPGraphpad pad = (GPGraphpad) parent;
		if (pad != null) {
			JPopupMenu pop = pad.getBarFactory().createLibraryPopupMenu();
			pop.show(c, p.x, p.y);
		}
	}

	public ScrollablePanel addLibrary(String name) {
		ScrollablePanel tmp = new ScrollablePanel(name);
		combo.addItem(tmp);
		combo.setSelectedItem(tmp);
		return tmp;
	}

	public void openLibrary(Object state) {
		if (state instanceof Vector) {
			Vector v = (Vector) state;
			Object ident = v.get(0);
			if (ident != null && ident.equals(LIBRARY_FORMAT)) {
				addLibrary(v.get(1).toString());
				for (int i = 2; i < v.size(); i = i + 4) {
					Object cells = v.get(i);
					Object cs = v.get(i+1);
					Object map = v.get(i+2);
					Object bounds = v.get(i+3);
					GPTransferable transferable =
						new GPTransferable(
							"",
							(Object[]) cells,
							(Map) map,
							(Rectangle) bounds,
							(ConnectionSet) cs,
							(ParentMap) null);
					getTransferHandler().importData(this, transferable);
				}
			}
		}
	}

	public void closeLibrary() {
		if (combo.getItemCount() > 1) {
			combo.removeItem(panel);
			combo.setSelectedIndex(0);
		}
	}

	public class ScrollablePanel extends JPanel implements Scrollable {

		protected GPGraph graph = new GPGraph();
		protected String name;

		public ScrollablePanel(String name) {
			super(new FixedWidthFlowLayout());
			setBackground(Color.white);
			this.name = name;
		}

		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		public int getScrollableBlockIncrement(
			Rectangle visibleRect,
			int orientation,
			int direction) {
			return 10;
		}

		public boolean getScrollableTracksViewportHeight() {
			return false;
		}

		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		public int getScrollableUnitIncrement(
			Rectangle visibleRect,
			int orientation,
			int direction) {
			return 10;
		}

		public GPGraph getGraph() {
			return graph;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}

		public Serializable getArchiveableState() {
			Vector v = new Vector();
			v.add(LIBRARY_FORMAT);
			v.add(name);
			Component[] comps = getComponents();
			for (int i = 0; i < comps.length; i++)
				if (comps[i] instanceof GraphCellsComponent) {
					GPTransferable transferable =
						(GPTransferable) ((GraphCellsComponent) comps[i])
							.getTransferable();
					v.add(transferable.getCells());
					v.add(transferable.getConnectionSet());
					v.add(transferable.getAttributeMap());
					v.add(transferable.getBounds());
				}
			return v;
		}

	}

	public void setSelected(GraphCellsComponent comp) {
		if (selected != null && oldBorder != null)
			selected.setBorder(oldBorder);
		selected = comp;
		if (selected != null && hasFocus()) {
			oldBorder = selected.getBorder();
			selected.setBorder(BorderFactory.createLoweredBevelBorder());
		}
	}

	/** Delete element from library.  Caller is responsible for any warning messages
	 * as this method immediately deletes without any prompts.
	 */
	public void delete() {
		if (selected != null && fullLibraryAccess) {
			GraphCellsComponent tmp = selected;
			GPTransferable t = tmp.getTransferable();
			panel.getGraph().getModel().remove(t.getCells());
			panel.remove(tmp);
			setSelected(null);
			panel.revalidate();
			repaint();
		}
	}

	public ScrollablePanel getPanel() {
		return panel;
	}

	class DNDTransferHandler extends TransferHandler {

		/**
		 * Create a Transferable to use as the source for a data transfer.
		 *
		 * @param buttonCircle  The component holding the data to be transfered.  This
		 *  argument is provided to enable sharing of TransferHandlers by
		 *  multiple components.
		 * @return  The representation of the data to be transfered.
		 *
		 */
		protected Transferable createTransferable(JComponent c) {
			if (selected != null) {
				dragging = true;
				return selected.getTransferable();
			}
			return null;
		}

		public int getSourceActions(JComponent c) {
			return COPY_OR_MOVE;
		}

		public boolean canImport(JComponent comp, DataFlavor[] flavors) {
			return true;
		}

		protected void exportDone(
			JComponent source,
			Transferable data,
			int action) {
			dragging = false;
		}

		public boolean importData(JComponent comp, Transferable t) {
			if (t.isDataFlavorSupported(GraphTransferable.dataFlavor)
				&& !dragging) {
				try {
					Object obj =
						t.getTransferData(GraphTransferable.dataFlavor);
					if (obj instanceof GraphTransferable && !dragging) {
						GraphTransferable gt = (GraphTransferable) obj;
						
						// more stabile code:
						// precondition tests for the parameters
						
						// get the bounds for the library entry
						Rectangle bounds;
						if (gt.getBounds() != null){
							bounds = gt.getBounds();
						} else {
							bounds = new Rectangle();
						}
						
						// get the cells for the library entry
						Map clones;
						if (gt.getCells() != null){
							clones = panel.getGraph().cloneCells(gt.getCells());						
						} else {
							clones = new Hashtable();
						}
						 
						// get the connections
						ConnectionSet cs;
						if (gt.getConnectionSet() != null){
							cs = gt.getConnectionSet().clone(clones);
						} else {
							cs = new ConnectionSet();
						}
						
						// get the parent map
						ParentMap pm;
						if (gt.getParentMap() != null){
							pm = gt.getParentMap().clone(clones);
						} else {
							pm = new ParentMap();
						}
						
						// get the attributes
						Map attributes;
						if (gt.getAttributeMap() != null){
							attributes = gt.getAttributeMap();
						} else {
							attributes = new Hashtable();
						}
						
						// replays the keys 
						attributes =
							GraphConstants.replaceKeys(clones, attributes);
							
						// extract the cells
						Object[] cells = clones.values().toArray();
						
						// inserts the cells
						panel.getGraph().getModel().insert(
							cells,
							attributes,
							cs,
							pm,
							null);
							
						// Translate to (0,0)
						for (int i = 0; i < cells.length; i++) {
							CellView view =
								panel
									.getGraph()
									.getGraphLayoutCache()
									.getMapping(
									cells[i],
									true);
							if (attributes != null) {
								Map map = (Map) attributes.get(cells[i]);
								if (map != null) {
									map = GraphConstants.cloneMap(map);
									GraphConstants.translate(
										map,
										-bounds.x,
										-bounds.y);
									view.setAttributes(map);
								}
							}
						}
						// Create new Transferable
						Object[] orig = gt.getCells();
						cells = new Object[orig.length];
						for (int i = 0; i < orig.length; i++)
							cells[i] = clones.get(orig[i]);
						String text = gt.getPlainData();
						GPTransferable newt =
							new GPTransferable(
								text,
								cells,
								attributes,
								bounds,
								cs,
								pm);
						panel.add(
							new GraphCellsComponent(
								panel.getGraph(),
								cells,
								newt));
						panel.revalidate();
					}
				} catch (Exception ex) {
					System.out.println("Error: " + ex.getMessage());
					ex.printStackTrace();
				}
			}
			return false;
		}
	}

	public static class FixedWidthFlowLayout extends FlowLayout {

		public FixedWidthFlowLayout() {
			super(FlowLayout.LEFT);
		}

		public Dimension preferredLayoutSize(Container target) {
			synchronized (target.getTreeLock()) {
				int maxw = target.getParent().getSize().width;
				int vgap = getVgap();
				int hgap = getHgap();
				Dimension dim = new Dimension(maxw, 0);
				int nmembers = target.getComponentCount();
				int w = 0, h = 0;
				for (int i = 0; i < nmembers; i++) {
					Component m = target.getComponent(i);
					if (m.isVisible()) {
						Dimension d = m.getPreferredSize();
						w += d.width + hgap;
						if (w >= maxw) {
							dim.height += h + vgap;
							w = d.width + hgap;
							h = 0;
						}
						h = Math.max(h, d.height);
					}
				}
				Insets insets = target.getInsets();
				dim.height += h + insets.top + insets.bottom + vgap * 2;
				return dim;
			}
		}

	}

	public class GraphCellsComponent extends JComponent {

		protected GPGraph graph;
		protected GPTransferable trans;
		protected Object[] cells;
		protected RealGraphCellRenderer renderer;
		protected double scale = 0.5;

		public GraphCellsComponent(
			GPGraph graph,
			Object[] cells,
			GPTransferable trans) {
			this.trans = trans;
			this.graph = graph;
			// Insert GraphCellRenderer
			setBorder(BorderFactory.createLineBorder(graph.getMarqueeColor()));
			CellView[] views =
				panel.getGraph().getGraphLayoutCache().getMapping(cells);
			renderer = new RealGraphCellRenderer(graph, views);
			renderer.setLocation(new Point(4, 4));
			Rectangle b = AbstractCellView.getBounds(views);
			int tmp = Math.max(b.width, b.height);
			if (tmp > MAX) {
				scale = (double) MAX / Math.max(b.width, b.height);
				renderer.setScale(scale);
				b.x *= scale;
				b.y *= scale;
				b.width *= scale;
				b.height *= scale;
			}
			setPreferredSize(new Dimension(b.width + 8, b.height + 8));
			renderer.setSize(getPreferredSize());
			add(renderer);
			addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (!isPopupTrigger(e)) {
						GPLibraryPanel.this.requestFocus();
						GPLibraryPanel.this.setSelected(
							GraphCellsComponent.this);
						GPLibraryPanel.this.getTransferHandler().exportAsDrag(
							GPLibraryPanel.this,
							e,
							TransferHandler.COPY);
					}
				}
				public void mouseReleased(MouseEvent e) {
					if (isPopupTrigger(e))
						showPopupMenu(panel, e.getPoint());
				}
			});
		}

		public GPTransferable getTransferable() {
			return trans;
		}

	}

}
