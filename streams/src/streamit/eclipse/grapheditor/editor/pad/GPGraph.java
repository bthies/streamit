/*
 * @(#)GPGraph.java	1.2 11/11/02
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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.Border;

import org.jgraph.JGraph;
import org.jgraph.graph.CellMapper;
import org.jgraph.graph.CellView;
import org.jgraph.graph.CellViewRenderer;
import org.jgraph.graph.DefaultGraphCellEditor;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.Edge;
import org.jgraph.graph.GraphCellEditor;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.Port;
import org.jgraph.graph.VertexRenderer;
import org.jgraph.graph.VertexView;

public class GPGraph extends JGraph {
	
	public static String FILE_FORMAT_VERSION = "PAD-1.0";

	protected boolean pagevisible = false;

	protected transient PageFormat pageFormat = new PageFormat();

	protected Image background = null;

	protected Hashtable writeProperties = new Hashtable();

	protected transient Color defaultBorderColor = Color.black;

	public GPGraph() {
		this(null);
	}

	public GPGraph(GraphModel model) {
		this(model, null);
	}

	public GPGraph(GraphModel model, GraphLayoutCache view) {
		super(model, view, null);

	}

	public Serializable getArchiveableState() {
		Object[] cells = getGraphLayoutCache().order(getAll());
		Map viewAttributes =
			GraphConstants.createAttributes(cells, getGraphLayoutCache());
		Vector v = new Vector();
		v.add(FILE_FORMAT_VERSION);
		v.add(cells);
		v.add(viewAttributes);
		v.add(writeProperties);
		v.add(new Boolean(isEditable()));
		v.add(new Boolean(isBendable()));
		v.add(new Boolean(isCloneable()));
		v.add(new Boolean(isConnectable()));
		v.add(new Boolean(isDisconnectable()));
		v.add(new Boolean(isDisconnectOnMove()));
		v.add(new Boolean(isDoubleBuffered()));
		v.add(new Boolean(isDragEnabled()));
		v.add(new Boolean(isDropEnabled()));
		v.add(new Boolean(isMoveable()));
		v.add(new Boolean(isPageVisible()));
		v.add(new Boolean(isSelectNewCells()));
		v.add(new Boolean(isSizeable()));
		v.add(new Boolean(isGridVisible()));
		v.add(new Boolean(isGridEnabled()));
		v.add(new Integer(getGridSize()));
		v.add(new Integer(getGridMode()));
		v.add(new Double(getScale()));
		//v.add(new Boolean(isAntiAliased())); next file version...
		return v;
	}

	public void setArchivedState(Object s) {
		if (s instanceof Vector) {
			Vector v = (Vector) s;
			
			// Supports Multiple Versions
			Object ident = v.get(0);
			
			if (ident instanceof String) {
				
				if (ident.equals(FILE_FORMAT_VERSION)) { // Current File Format
					int index = 1;
					Object[] cells = (Object[]) v.get(index++);
					Map attrib = (Map) v.get(index++);
					getGraphLayoutCache().insert(cells, attrib, null, null, null);
					setWriteProperties((Hashtable)v.get(index++));
					setEditable(((Boolean) (v.get(index++))).booleanValue());
					setBendable(((Boolean) (v.get(index++))).booleanValue());
					setCloneable(((Boolean) (v.get(index++))).booleanValue());
					setConnectable(((Boolean) (v.get(index++))).booleanValue());
					setDisconnectable(((Boolean) (v.get(index++))).booleanValue());
					setDisconnectOnMove(((Boolean) (v.get(index++))).booleanValue());
					setDoubleBuffered(((Boolean) (v.get(index++))).booleanValue());
					setDragEnabled(((Boolean) (v.get(index++))).booleanValue());
					setDropEnabled(((Boolean) (v.get(index++))).booleanValue());
					setMoveable(((Boolean) (v.get(index++))).booleanValue());
					setPageVisible(((Boolean) (v.get(index++))).booleanValue());
					setSelectNewCells(((Boolean) (v.get(index++))).booleanValue());
					setSizeable(((Boolean) (v.get(index++))).booleanValue());
					setGridVisible(((Boolean) (v.get(index++))).booleanValue());
					setGridEnabled(((Boolean) (v.get(index++))).booleanValue());
					setGridSize(((Integer) (v.get(index++))).intValue());
					setGridMode(((Integer) (v.get(index++))).intValue());
					setScale(((Double) (v.get(index++))).doubleValue());
					//setAntiAliased(((Boolean) (v.get(index++))).booleanValue()); next file version...
				} else
					System.out.println("Unknown file format: "+ident);
				
			} else {
				// Unversioned (old) file format
				Object[] cells = (Object[]) v.get(0);
				Map attrib = (Map) v.get(1);
				getGraphLayoutCache().insert(cells, attrib, null, null, null);
				setWriteProperties((Hashtable)v.get(2));
			}
		}
	}

	public void setPageFormat(PageFormat format) {
		pageFormat = format;
	}

	public PageFormat getPageFormat() {
		return pageFormat;
	}

	public void setPageVisible(boolean flag) {
		pagevisible = flag;
	}

	public boolean isPageVisible() {
		return pagevisible;
	}

	public void setBackgroundImage(Image img) {
		background = img;
	}

	public Image getBackgroundImage() {
		return background;
	}

	/**
	 * Creates and returns a default <code>GraphView</code>.
	 *
	 * @return the default <code>GraphView</code>
	 */
	protected VertexView createVertexView(Object v, CellMapper cm) {
		if (v instanceof EllipseCell)
			return new EllipseView(v, this, cm);
		else if (v instanceof ImageCell)
			return new ScaledVertexView(v, this, cm);
		else if ((v instanceof TextCell) &&
		 		  ((TextCell) v).isMultiLined())
		 	return new MultiLinedView(v, this, cm);
		return super.createVertexView(v, cm);
	}

	public static VertexRenderer renderer = new ScaledVertexRenderer();

	public class ScaledVertexView extends VertexView {

		public ScaledVertexView(Object v, JGraph graph, CellMapper cm) {
			super(v, graph, cm);
		}

		public CellViewRenderer getRenderer() {
			return GPGraph.renderer;
		}

	}

	public static class ScaledVertexRenderer extends VertexRenderer {

		public void paint(Graphics g) {
			Icon icon = getIcon();
			setIcon(null);
			Dimension d = getSize();
			Image img = null;
			if (icon instanceof ImageIcon)
				img = ((ImageIcon) icon).getImage();
			if (img != null)
				g.drawImage(img, 0, 0, d.width - 1, d.height - 1, graph);
			super.paint(g);
		}

	}

	//
	// Defines Semantics of the Graph
	//

	/**
	 * Returns true if <code>object</code> is a vertex, that is, if it
	 * is not an instance of Port or Edge, and all of its children are
	 * ports, or it has no children.
	 */
	public boolean isGroup(Object cell) {
		// Map the Cell to its View
		CellView view = getGraphLayoutCache().getMapping(cell, false);
		if (view != null)
			return !view.isLeaf();
		return false;
	}

	/**
	 * Returns true if <code>object</code> is a vertex, that is, if it
	 * is not an instance of Port or Edge, and all of its children are
	 * ports, or it has no children.
	 */
	public boolean isVertex(Object object) {
		if (!(object instanceof Port) && !(object instanceof Edge))
			return !isGroup(object) && object != null;
		return false;
	}

	public boolean isPort(Object object) {
		return (object instanceof Port);
	}

	public boolean isEdge(Object object) {
		return (object instanceof Edge);
	}

	public Object[] getSelectionVertices() {
		Object[] tmp = getSelectionCells();
		Object[] all =
			DefaultGraphModel.getDescendants(getModel(), tmp).toArray();
		return getVertices(all);
	}

	public Object[] getVertices(Object[] cells) {
		if (cells != null) {
			ArrayList result = new ArrayList();
			for (int i = 0; i < cells.length; i++)
				if (isVertex(cells[i]))
					result.add(cells[i]);
			return result.toArray();
		}
		return null;
	}

	public Object[] getSelectionEdges() {
		return getEdges(getSelectionCells());
	}

	public Object[] getAll() {
		return getDescendants(getRoots());
	}

	public Object[] getEdges(Object[] cells) {
		if (cells != null) {
			ArrayList result = new ArrayList();
			for (int i = 0; i < cells.length; i++)
				if (isEdge(cells[i]))
					result.add(cells[i]);
			return result.toArray();
		}
		return null;
	}

	public Object getNeighbour(Object edge, Object vertex) {
		Object source = getSourceVertex(edge);
		if (vertex == source)
			return getTargetVertex(edge);
		else
			return source;
	}

	public Object getSourceVertex(Object edge) {
		Object sourcePort = graphModel.getSource(edge);
		return graphModel.getParent(sourcePort);
	}

	public Object getTargetVertex(Object edge) {
		Object targetPort = graphModel.getTarget(edge);
		return graphModel.getParent(targetPort);
	}

	public CellView getSourceView(Object edge) {
		Object source = getSourceVertex(edge);
		return getGraphLayoutCache().getMapping(source, false);
	}

	public CellView getTargetView(Object edge) {
		Object target = getTargetVertex(edge);
		return getGraphLayoutCache().getMapping(target, false);
	}

	public Object[] getEdgesBetween(Object vertex1, Object vertex2) {
		ArrayList result = new ArrayList();
		Set edges =
			DefaultGraphModel.getEdges(graphModel, new Object[] { vertex1 });
		Iterator it = edges.iterator();
		while (it.hasNext()) {
			Object edge = it.next();
			Object source = getSourceVertex(edge);
			Object target = getTargetVertex(edge);
			if ((source == vertex1 && target == vertex2)
				|| (source == vertex2 && target == vertex1))
				result.add(edge);
		}
		return result.toArray();
	}

	/**
	 * Overrides <code>JComponent</code>'buttonSelect <code>getToolTipText</code>
	 * method in order to allow the graph controller to create a tooltip
	 * for the topmost cell under the mousepointer. This differs from JTree
	 * where the renderers tooltip is used.
	 * <p>
	 * NOTE: For <code>JGraph</code> to properly display tooltips of its
	 * renderers, <code>JGraph</code> must be a registered component with the
	 * <code>ToolTipManager</code>.  This can be done by invoking
	 * <code>ToolTipManager.sharedInstance().registerComponent(graph)</code>.
	 * This is not done automatically!
	 * @param event the <code>MouseEvent</code> that initiated the
	 * <code>ToolTip</code> display
	 * @return a string containing the  tooltip or <code>null</code>
	 * if <code>event</code> is null
	 */
	public String getToolTipText(MouseEvent event) {
		if (event != null) {
			Object cell = getFirstCellForLocation(event.getX(), event.getY());
			if (cell != null) {
				String tmp = convertValueToString(cell);
				String s = "<html>";
				if (tmp != null && tmp.length() > 0)
					s = s + "<strong>" + tmp + "</strong><br>";
				return s + getToolTipForCell(cell) + "</html>";
			}
		}
		return null;
	}

	protected String getToolTipForCell(Object cell) {
		CellView view = getGraphLayoutCache().getMapping(cell, false);
		String s = "";
		Rectangle bounds = view.getBounds();
		if (bounds != null) {
			s = s + "Location: " + bounds.x + ", " + bounds.y + "<br>";
			s = s + "Size: " + bounds.width + ", " + bounds.height + "<br>";
		}
		java.util.List points = GraphConstants.getPoints(view.getAttributes());
		if (points != null)
			s = s + "Points: " + points.size() + "<br>";
		if (!(cell instanceof Edge) && !(cell instanceof Port)) {
			s = s + "Children: " + graphModel.getChildCount(cell) + "<br>";
			int n =
				DefaultGraphModel
					.getEdges(getModel(), new Object[] { cell })
					.size();
			s = s + "Edges: " + n;
		} else if (cell instanceof Edge) {
			Edge edge = (Edge) cell;
			Object source = graphModel.getSource(edge);
			if (source != null) {
				String host =
					convertValueToString(graphModel.getParent(source));
				String port = convertValueToString(source);
				s = s + "Source: " + host + ":" + port + "<br>";
			}
			Object target = graphModel.getTarget(edge);
			if (target != null) {
				String host =
					convertValueToString(graphModel.getParent(target));
				String port = convertValueToString(target);
				s = s + "Target: " + host + "/" + port + "<br>";
			}
		}
		return s;
	}

	/**
	 * Notification from the <code>UIManager</code> that the L&F has changed.
	 * Replaces the current UI object with the latest version from the
	 * <code>UIManager</code>. Subclassers can override this to support
	 * different GraphUIs.
	 * @see JComponent#updateUI
	 *
	 */
	public void updateUI() {
		setUI(new GPGraphUI());
		invalidate();
	}


	/** Returns true if the given vertices are conntected by a single edge
	 * in this document.
	 */
	public boolean isNeighbour(Object v1, Object v2) {
		Object[] edges = getEdgesBetween(v1, v2);
		return (edges != null && edges.length > 0);
	}

	// Serialization support
	private void readObject(ObjectInputStream s)
		throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		pageFormat = new PageFormat();
	}

	//
	// Multiline View
	//

	class MultiLinedView extends VertexView {
		// static
		final MultiLinedRenderer renderer = new MultiLinedRenderer();
		// static
		final MultiLinedEditor editor = new MultiLinedEditor();

		public MultiLinedView(Object cell, JGraph graph, CellMapper cm) {
			super(cell, graph, cm);
		}

		public CellViewRenderer getRenderer() {
			return renderer;
		}

		public GraphCellEditor getEditor() {
			return editor;
		}

		class MultiLinedEditor extends DefaultGraphCellEditor {
			class RealCellEditor
				extends AbstractCellEditor
				implements GraphCellEditor {
				JTextArea editorComponent = new JTextArea();
				public RealCellEditor() {
					editorComponent.setBorder(
						UIManager.getBorder("Tree.editorBorder"));
					editorComponent.setLineWrap(true);
					editorComponent.setWrapStyleWord(true);

					//substitute a JTextArea's VK_ENTER action with our own that will stop an edit.
					editorComponent.getInputMap(JComponent.WHEN_FOCUSED).put(
						KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
						"enter");
					editorComponent
						.getActionMap()
						.put("enter", new AbstractAction() {
						public void actionPerformed(ActionEvent e) {
							stopCellEditing();
						}
					});
				}

				public Component getGraphCellEditorComponent(
					JGraph graph,
					Object value,
					boolean isSelected) {
					editorComponent.setText(value.toString());
					editorComponent.selectAll();
					return editorComponent;
				}

				public Object getCellEditorValue() {
					return editorComponent.getText();
				}

				public boolean stopCellEditing() {
					//set the size of a vertex to that of an editor.
					CellView view =
						graph.getGraphLayoutCache().getMapping(
							graph.getEditingCell(),
							false);
					Map map = view.getAllAttributes();
					Rectangle cellBounds = GraphConstants.getBounds(map);
					Rectangle editingBounds = editorComponent.getBounds();
					cellBounds.setBounds(cellBounds.x,
							cellBounds.y,
							editingBounds.width,
							editingBounds.height);
					// Call view.setBounds() to make sure the view stores
					// the new bounds
					return super.stopCellEditing();
				}

				public boolean shouldSelectCell(EventObject event) {
					editorComponent.requestFocus();
					return super.shouldSelectCell(event);
				}
			}

			public MultiLinedEditor() {
				super();
			}
			/**
			 * Overriding this in order to set the size of an editor to that of an edited view.
			 */
			public Component getGraphCellEditorComponent(
				JGraph graph,
				Object cell,
				boolean isSelected) {

				Component component =
					super.getGraphCellEditorComponent(graph, cell, isSelected);

				//set the size of an editor to that of a view
				CellView view =
					graph.getGraphLayoutCache().getMapping(cell, false);
				editingComponent.setBounds(view.getBounds());

				//I have to set a font here instead of in the RealCellEditor.getGraphCellEditorComponent() because
				//I don't know what cell is being edited when in the RealCellEditor.getGraphCellEditorComponent().
				Font font = GraphConstants.getFont(view.getAllAttributes());
				editingComponent.setFont(
					(font != null) ? font : graph.getFont());

				return component;
			}

			protected GraphCellEditor createGraphCellEditor() {
				return new MultiLinedEditor.RealCellEditor();
			}

			/**
			 * Overriting this so that I could modify an eiditor container.
			 * see http://sourceforge.net/forum/forum.php?thread_id=781479&forum_id=140880
			 */
			protected Container createContainer() {
				return new MultiLinedEditor.ModifiedEditorContainer();
			}

			class ModifiedEditorContainer extends EditorContainer {
				public void doLayout() {
					super.doLayout();
					//substract 2 pixels that were added to the preferred size of the container for the border.
					Dimension cSize = getSize();
					Dimension dim = editingComponent.getSize();
					editingComponent.setSize(dim.width - 2, dim.height);

					//reset container's size based on a potentially new preferred size of a real editor.
					setSize(cSize.width, getPreferredSize().height);
				}
			}
		}

		class MultiLinedRenderer
			extends JTextArea
			implements CellViewRenderer {
			{
				setLineWrap(true);
				setWrapStyleWord(true);
			}

			public Component getRendererComponent(
				JGraph graph,
				CellView view,
				boolean sel,
				boolean focus,
				boolean preview) {
				setText(view.getCell().toString());

				Map attributes = view.getAllAttributes();
				installAttributes(graph, attributes);
				return this;
			}

			protected void installAttributes(JGraph graph, Map attributes) {
				setOpaque(GraphConstants.isOpaque(attributes));
				Color foreground = GraphConstants.getForeground(attributes);
				setForeground(
					(foreground != null) ? foreground : graph.getForeground());
				Color background = GraphConstants.getBackground(attributes);
				setBackground(
					(background != null) ? background : graph.getBackground());
				Font font = GraphConstants.getFont(attributes);
				setFont((font != null) ? font : graph.getFont());
				Border border = GraphConstants.getBorder(attributes);
				Color bordercolor = GraphConstants.getBorderColor(attributes);
				if (border != null)
					setBorder(border);
				else if (bordercolor != null) {
					int borderWidth =
						Math.max(
							1,
							Math.round(
								GraphConstants.getLineWidth(attributes)));
					setBorder(
						BorderFactory.createLineBorder(
							bordercolor,
							borderWidth));
				}
			}
		}
	}


	/**
	 * Returns the writeProperties.
	 * @return Hashtable
	 */
	public Hashtable getWriteProperties() {
		return writeProperties;
	}

	/**
	 * Sets the writeProperties.
	 * @param writeProperties The writeProperties to set
	 */
	public void setWriteProperties(Hashtable writeProperties) {
		this.writeProperties = writeProperties;
	}

}