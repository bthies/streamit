/*
 * @(#)GPMarqueeHandler.java	1.2 05.02.2003
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
package streamit.eclipse.grapheditor.editor.pad;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.jgraph.graph.BasicMarqueeHandler;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.PortView;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.controllers.GEFeedbackLoopController;
import streamit.eclipse.grapheditor.editor.controllers.GEFilterController;
import streamit.eclipse.grapheditor.editor.controllers.GEJoinerController;
import streamit.eclipse.grapheditor.editor.controllers.GEPipelineController;
import streamit.eclipse.grapheditor.editor.controllers.GESplitJoinController;
import streamit.eclipse.grapheditor.editor.controllers.GESplitterController;
import streamit.eclipse.grapheditor.editor.controllers.StreamTypeDialog;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.NodeCreator;
import streamit.eclipse.grapheditor.graph.NodeTemplateGenerator;
import streamit.eclipse.grapheditor.graph.RelativePosition;

/**
 * MarqueeHandler that can insert cells.
 *
 * @author sven.luzar
 * @version 1.0
 *
 */
public class GPMarqueeHandler extends BasicMarqueeHandler {

	int m_XDifference, m_YDifference, dx, dy;
	boolean m_dragging;
	Container c;

	/** A reference to the graphpad object
	 */
	GPGraphpad graphpad;

	/** The default color for borders
	 */
	protected transient Color defaultBorderColor = Color.black;

	protected transient JToggleButton buttonSelect = new JToggleButton();
	protected transient JToggleButton buttonRectangle = new JToggleButton();
	protected transient JToggleButton buttonFilter = new JToggleButton();
	protected transient JToggleButton buttonSplitter = new JToggleButton();
	protected transient JToggleButton buttonJoiner = new JToggleButton();
	protected transient JToggleButton buttonSplitJoin = new JToggleButton();
	protected transient JToggleButton buttonFeedbackLoop = new JToggleButton();
	
	protected transient JToggleButton buttonNode = new JToggleButton();
	protected transient JToggleButton buttonEdge = new JToggleButton();
	protected transient JToggleButton buttonZoomArea = new JToggleButton();
	
	protected Point start, current;
	protected Rectangle bounds;
	protected PortView port, firstPort, lastPort;

	/**
	 * Constructor for GPMarqueeHandler.
	 */
	public GPMarqueeHandler(GPGraphpad graphpad)
	 {
		super();
		this.graphpad = graphpad;
		ButtonGroup grp = new ButtonGroup();
		grp.add(buttonSelect);
		grp.add(buttonRectangle);
		grp.add(buttonFilter);
		grp.add(buttonSplitter);
		grp.add(buttonJoiner);
		grp.add(buttonSplitJoin);
		grp.add(buttonFeedbackLoop);
		grp.add(buttonNode);
		grp.add(buttonEdge);
		grp.add(buttonZoomArea);
	}

	/* Return true if this handler should be preferred over other handlers. */
	public boolean isForceMarqueeEvent(MouseEvent e) 
	{
		return !buttonSelect.isSelected()
			|| isPopupTrigger(e)
			|| super.isForceMarqueeEvent(e);
	}

	protected boolean isPopupTrigger(MouseEvent e) 
	{
		if (e==null) return false;
		return SwingUtilities.isRightMouseButton(e) && !e.isShiftDown();
	}

	public void mousePressed(MouseEvent event) 
	{
		m_XDifference = event.getX();
		m_YDifference = event.getY();
		dx = 0;
		dy = 0;
		
		/**
		 * Modified by jcarlos
		 */
		/*
		GPGraph gGraph = graphpad.getCurrentGraph();
		
		if (event.getButton() == MouseEvent.BUTTON3)
		{	
			if (gGraph.getFirstCellForLocation(m_XDifference, m_YDifference) instanceof GEStreamNode)
			{
				System.out.println("EVENT SUCCESS");
				GEStreamNode node = (GEStreamNode) gGraph.getFirstCellForLocation(m_XDifference,m_YDifference);
				
				if (node != null)
				{
					node.collapseExpand(gGraph);
				}
		
			}
		}
		else {
		*/
		

			
		if (!isPopupTrigger(event)
			&& !event.isConsumed()
			&& !buttonSelect.isSelected()) {
			start = graphpad.getCurrentGraph().snap(event.getPoint());
			firstPort = port;
			if (buttonEdge.isSelected() && firstPort != null)
				start =
					graphpad.getCurrentGraph().toScreen(
						firstPort.getLocation(null));
			event.consume();
		}
		if (!isPopupTrigger(event))
			super.mousePressed(event);
		else {
			boolean selected = false;
			Object[] cells = graphpad.getCurrentGraph().getSelectionCells();
			for (int i = 0; i < cells.length && !selected; i++)
				selected =
					graphpad.getCurrentGraph().getCellBounds(
						cells[i]).contains(
						event.getPoint());
			if (!selected)
				graphpad.getCurrentGraph().setSelectionCell(
					graphpad.getCurrentGraph().getFirstCellForLocation(
						event.getX(),
						event.getY()));
			event.consume();
		}
		
	}

	public void mouseDragged(MouseEvent event) {
	
		if (!event.isConsumed() && !buttonSelect.isSelected()) 
		{
			
			Graphics g = graphpad.getCurrentGraph().getGraphics();
			Color bg = graphpad.getCurrentGraph().getBackground();
			Color fg = Color.black;
			g.setColor(fg);
			g.setXORMode(bg);
			overlay(g);
			current = graphpad.getCurrentGraph().snap(event.getPoint());
			if (buttonEdge.isSelected()) {
				port =
					getPortViewAt(
						event.getX(),
						event.getY(),
						!event.isShiftDown());
				if (port != null)
					current =
						graphpad.getCurrentGraph().toScreen(
							port.getLocation(null));
			}
			bounds = new Rectangle(start).union(new Rectangle(current));
			g.setColor(bg);
			g.setXORMode(fg);
			overlay(g);
			event.consume();
		} 
		else if (!event.isConsumed() && isForceMarqueeEvent(event) && isPopupTrigger(event)) 
		{
			
			c = graphpad.getCurrentGraph().getParent();
			if (c instanceof JViewport) {
				JViewport jv = (JViewport) c;
				Point p = jv.getViewPosition();
				int newX = p.x - (event.getX() - m_XDifference);
				int newY = p.y - (event.getY() - m_YDifference);
				dx += (event.getX() - m_XDifference);
				dy += (event.getY() - m_YDifference);

				int maxX =
					graphpad.getCurrentGraph().getWidth() - jv.getWidth();
				int maxY =
					graphpad.getCurrentGraph().getHeight() - jv.getHeight();
				if (newX < 0)
					newX = 0;
				if (newX > maxX)
					newX = maxX;
				if (newY < 0)
					newY = 0;
				if (newY > maxY)
					newY = maxY;

				jv.setViewPosition(new Point(newX, newY));
				event.consume();
			}
		}
		
		super.mouseDragged(event);
	}

	// Default Port is at index 0
	public PortView getPortViewAt(int x, int y, boolean jump) 
	{
	
		Point sp = graphpad.getCurrentGraph().fromScreen(new Point(x, y));
		PortView port = graphpad.getCurrentGraph().getPortViewAt(sp.x, sp.y);
		// Shift Jumps to "Default" Port (child index 0)
		if (port == null && jump) {
			Object cell =
				graphpad.getCurrentGraph().getFirstCellForLocation(x, y);
			if (graphpad.getCurrentGraph().isVertex(cell)) {
				Object firstChild =
					graphpad.getCurrentGraph().getModel().getChild(cell, 0);
				CellView firstChildView =
					graphpad
						.getCurrentGraph()
						.getGraphLayoutCache()
						.getMapping(
						firstChild,
						false);
				if (firstChildView instanceof PortView)
					port = (PortView) firstChildView;
			}
		}
		return port;
	}

	/**
	 * Get the GEStreamNode that is at Point p.
	 * @param p Point 
	 * @return GEStreamNode located at Point p. If there is no GEStreamNode at that
	 * location, then return null.
	 */
	public GEStreamNode getGEStreamNode(Point p)
	{
		
		if (graphpad.getCurrentGraph().getFirstCellForLocation(p.x, p.y) instanceof GEStreamNode)
		{
			return (GEStreamNode) graphpad.getCurrentGraph().getFirstCellForLocation(p.x, p.y);
		}
		else 
		{
			return null;
		}
	}



	public void mouseReleased(MouseEvent event) {

		// precondition test
		/* we don't want to have a 
		 * Default Esc Action 
		 
		if (event == null) {
			Action a =
				graphpad.getCurrentActionMap().get(
					Utilities.getClassNameWithoutPackage(FileClose.class));
			if (a != null)
				a.actionPerformed(new ActionEvent(this, 0, "FileClose"));
			return;

		}
		*/

		GraphModelProvider gmp = graphpad.getCurrentGraphModelProvider();
		GraphModel model = graphpad.getCurrentGraph().getModel();

		if (isPopupTrigger(event)) {
			
			if (Math.abs(dx) < graphpad.getCurrentGraph().getTolerance()
				&& Math.abs(dy) < graphpad.getCurrentGraph().getTolerance()) {
				Object cell =
					graphpad.getCurrentGraph().getFirstCellForLocation(
						event.getX(),
						event.getY());
				if (cell == null)
					graphpad.getCurrentGraph().clearSelection();
				Container parent = graphpad.getCurrentGraph();
				do {
					parent = parent.getParent();
				} while (parent != null && !(parent instanceof GPGraphpad));

				GPGraphpad pad = (GPGraphpad) parent;
				if (pad != null) {
					JPopupMenu pop = pad.getBarFactory().createGraphPopupMenu();

					final GPGraph graph =
						graphpad.getCurrentDocument().getGraph();
					final Object selectionCell = graph.getSelectionCell();

					if (selectionCell instanceof DefaultGraphCell) {
						final Object userObject =
							((DefaultGraphCell) selectionCell).getUserObject();
						if (userObject instanceof GPUserObject) {
							JMenuItem mi = new JMenuItem("Properties");
							pop.addSeparator();
							pop.add(mi);
							mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									(
										(
											GPUserObject) userObject)
												.showPropertyDialog(
										graph,
										selectionCell);
								}
							});
						}
					}
					pop.show(
						graphpad.getCurrentGraph(),
						event.getX(),
						event.getY());
				}
			}
			event.consume();
			
		} 
		else if (event != null && !event.isConsumed() && bounds != null && !buttonSelect.isSelected()) 
		{
			graphpad.getCurrentGraph().fromScreen(bounds);
			bounds.width++;
			bounds.height++;
			if (buttonZoomArea.isSelected()) {
				Rectangle view = graphpad.getCurrentGraph().getBounds();
				if (graphpad.getCurrentGraph().getParent()
					instanceof JViewport)
					view =
						((JViewport) graphpad.getCurrentGraph().getParent())
							.getViewRect();
				if (bounds.width != 0
					&& bounds.height != 0
					&& SwingUtilities.isLeftMouseButton(event)) {
					double scale =
						Math.min(
							(double) view.width / (double) bounds.width,
							(double) view.height / (double) bounds.height);
					if (scale > 0.1) {
						Rectangle unzoomed =
							graphpad.getCurrentGraph().fromScreen(bounds);
						graphpad.getCurrentGraph().setScale(scale);
						graphpad.getCurrentGraph().scrollRectToVisible(
							graphpad.getCurrentGraph().toScreen(unzoomed));
					}
				} else
					graphpad.getCurrentGraph().setScale(1);
				// FIX: Set ResizeAction to null!
			} 
			
			else if (buttonRectangle.isSelected())
			{
				GEPipelineController pipelineController = new GEPipelineController();
				if (pipelineController.configure(graphpad.getCurrentDocument()))
				{
			
					Properties pipelineProperties = pipelineController.getConfiguration();
					NodeCreator.nodeCreate(pipelineProperties,
											graphpad.getCurrentDocument().getGraph(),
											bounds,
											graphpad.getCurrentDocument().getGraphStructure());
					String template = NodeTemplateGenerator.createTemplateCode(pipelineProperties);
					writeTemplateCodeToFile(template);
				}
				else 
				{
					graphpad.getCurrentGraph().getModel().insert(null,null,null,null,null);	
				}				
			}
			else if (buttonFilter.isSelected())
			{
				GEFilterController filterController = new GEFilterController();
				if (filterController.configure(graphpad.getCurrentDocument()))
				{
			
					Properties filterProperties = filterController.getConfiguration();
					NodeCreator.nodeCreate(filterProperties,
											graphpad.getCurrentDocument().getGraph(),
											bounds,
											graphpad.getCurrentDocument().getGraphStructure());
					String template = NodeTemplateGenerator.createTemplateCode(filterProperties);
					writeTemplateCodeToFile(template);
				}
				else 
				{
					graphpad.getCurrentGraph().getModel().insert(null,null,null,null,null);	
				}
				
			}
			else if (buttonSplitter.isSelected()) 
			{
				GESplitterController splitterController = new GESplitterController();
				if (splitterController.configure(graphpad.getCurrentDocument()))
				{
			
					Properties splitterProperties = splitterController.getConfiguration();
					NodeCreator.nodeCreate(splitterProperties,
											graphpad.getCurrentDocument().getGraph(),
											bounds,
											graphpad.getCurrentDocument().getGraphStructure());
					String template = NodeTemplateGenerator.createTemplateCode(splitterProperties);
					writeTemplateCodeToFile(template);
				}
				else 
				{
					graphpad.getCurrentGraph().getModel().insert(null,null,null,null,null);	
				}
				
				//graphpad.getCurrentGraph().startEditingAtCell(cell);
			} 
			else if (buttonJoiner.isSelected())
			{
				GEJoinerController joinerController = new GEJoinerController();
				if (joinerController.configure(graphpad.getCurrentDocument()))
				{
			
					Properties joinerProperties = joinerController.getConfiguration();
					NodeCreator.nodeCreate(joinerProperties,
											graphpad.getCurrentDocument().getGraph(),
											bounds,
											graphpad.getCurrentDocument().getGraphStructure());
					String template = NodeTemplateGenerator.createTemplateCode(joinerProperties);
					writeTemplateCodeToFile(template);
				}
				else 
				{
					graphpad.getCurrentGraph().getModel().insert(null,null,null,null,null);	
				}				
			}
			else if (buttonSplitJoin.isSelected())
			{
				
				GESplitJoinController SplitJoinController = new GESplitJoinController();
				if (SplitJoinController.configure(graphpad.getCurrentDocument()))
				{
			
					Properties splitJoinProperties = SplitJoinController.getConfiguration();
					NodeCreator.nodeCreate(splitJoinProperties,
											graphpad.getCurrentDocument().getGraph(),
											bounds,
											graphpad.getCurrentDocument().getGraphStructure());
					String template = NodeTemplateGenerator.createTemplateCode(splitJoinProperties);
					writeTemplateCodeToFile(template);
				}
				else 
				{
					graphpad.getCurrentGraph().getModel().insert(null,null,null,null,null);	
				}		
			}
			else if (buttonFeedbackLoop.isSelected())
			{
				GEFeedbackLoopController feedbackloopController = new GEFeedbackLoopController();
				if (feedbackloopController.configure(graphpad.getCurrentDocument()))
				{
			
					Properties feedbackloopProperties = feedbackloopController.getConfiguration();
					NodeCreator.nodeCreate(feedbackloopProperties,
											graphpad.getCurrentDocument().getGraph(),
											bounds,
											graphpad.getCurrentDocument().getGraphStructure());
					String template = NodeTemplateGenerator.createTemplateCode(feedbackloopProperties);
					writeTemplateCodeToFile(template);
				}
				else 
				{
					graphpad.getCurrentGraph().getModel().insert(null,null,null,null,null);	
				}						
			}
	
			else if (buttonNode.isSelected()) 
			{
				Frame f = JOptionPane.getFrameForComponent(graphpad);
				final StreamTypeDialog std = new StreamTypeDialog(f,graphpad.getCurrentDocument());
				std.show();
				
				if (std.isCanceled())
				{
					graphpad.getCurrentGraph().getModel().insert(null,null,null,null,null);	
				}
				else 
				{
					Properties newNodeProperties = std.getConfiguration();
					System.out.println("Properties : " + newNodeProperties.toString());
					NodeCreator.nodeCreate(newNodeProperties, 
										   graphpad.getCurrentDocument().getGraph(),
										   bounds,
										   graphpad.getCurrentDocument().getGraphStructure());
					
					String template = NodeTemplateGenerator.createTemplateCode(newNodeProperties);
					writeTemplateCodeToFile(template);
					
				}									
			} 
			else if (buttonEdge.isSelected()) 
			{
				Point p = graphpad.getCurrentGraph().fromScreen(new Point(start));
				Point p2 = graphpad.getCurrentGraph().fromScreen(new Point(current));
				
				GEStreamNode nodeStart = getGEStreamNode(new Point(start));	
				GEStreamNode nodeEnd = getGEStreamNode(new Point(current));
				
				if ((nodeStart != null) && (nodeEnd !=null) && (nodeStart != nodeEnd))
				{
					int relativePos = RelativePosition.BOTH_PRESENT;
					if ((!nodeStart.isNodeConnected()) && (!nodeEnd.isNodeConnected()))
					{
						relativePos = RelativePosition.NONE_PRESENT;
					}
					else if ((!nodeStart.isNodeConnected()) && (nodeEnd.isNodeConnected()))
					{
						relativePos = RelativePosition.END_PRESENT;
					}
					else if ((nodeStart.isNodeConnected()) && (!nodeEnd.isNodeConnected()))
					{
						relativePos = RelativePosition.START_PRESENT;	
					}
					else if ((nodeStart.isNodeConnected()) && (nodeEnd.isNodeConnected()))
					{
						relativePos = RelativePosition.BOTH_PRESENT;						
					}
		
					if (!(graphpad.getCurrentDocument().getGraphStructure().connect(nodeStart,nodeEnd, relativePos)))
					{
						graphpad.getCurrentGraph().getModel().insert(null,null,null,null,null);				
					}
				}
				else
				{
					graphpad.getCurrentGraph().getModel().insert(null,null,null,null,null);
				}
				
			}
			event.consume();
		}
		buttonSelect.doClick();
		firstPort = null;
		port = null;
		start = null;
		current = null;
		bounds = null;
		super.mouseReleased(event);
	
	
	}

	public void mouseMoved(MouseEvent event) 
	{
		
		if (!buttonSelect.isSelected() && !event.isConsumed()) {
			graphpad.getCurrentGraph().setCursor(
				new Cursor(Cursor.CROSSHAIR_CURSOR));
			event.consume();
			if (buttonEdge.isSelected()) {
				PortView oldPort = port;
				PortView newPort =
					getPortViewAt(
						event.getX(),
						event.getY(),
						!event.isShiftDown());
				if (oldPort != newPort) {
					Graphics g = graphpad.getCurrentGraph().getGraphics();
					Color bg = graphpad.getCurrentGraph().getBackground();
					Color fg = graphpad.getCurrentGraph().getMarqueeColor();
					g.setColor(fg);
					g.setXORMode(bg);
					overlay(g);
					port = newPort;
					g.setColor(bg);
					g.setXORMode(fg);
					overlay(g);
				}
			}
		}
		super.mouseMoved(event);
	}

	public void overlay(Graphics g) 
	{
		
		super.overlay(g);
		paintPort(graphpad.getCurrentGraph().getGraphics());
		if (bounds != null && start != null) {
			if (buttonZoomArea.isSelected())
				 ((Graphics2D) g).setStroke(GraphConstants.SELECTION_STROKE);
			if (buttonEdge.isSelected() && current != null)
				g.drawLine(start.x, start.y, current.x, current.y);
			else if (!buttonSelect.isSelected())
				g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
		}
	}

	protected void paintPort(Graphics g) {
		
		if (port != null) {
			boolean offset =
				(GraphConstants.getOffset(port.getAllAttributes()) != null);
			Rectangle r =
				(offset) ? port.getBounds() : port.getParentView().getBounds();
			r = graphpad.getCurrentGraph().toScreen(new Rectangle(r));
			int s = 3;
			r.translate(-s, -s);
			r.setSize(r.width + 2 * s, r.height + 2 * s);
			GPGraphUI ui = (GPGraphUI) graphpad.getCurrentGraph().getUI();
			ui.paintCell(g, port, r, true);
		
		}
	}

	/**
	 * Add a GEStreamnode vertex to GraphStructure so that it can be made visible. 
	 * @param type The type of the GEStreamNode vertex to be added.
	 * @param name The name of the GEStreamNode vertex to be added.
	 * @param bounds The bound (location) at which the vertext will be placed.
	 * @param properties The Protperties that need to be applied .
	 * @return
	 */
	/*
	public Object createNode(Properties properties, Rectangle bounds)
	{
		GPGraph ggg = graphpad.getCurrentGraph();
		GraphLayoutCache glc = ggg.getGraphLayoutCache();
		GEStreamNode node = null;
		
		String name = properties.getProperty(GEProperties.KEY_NAME);
		
		//CAnged to Test
		String type = properties.getProperty(GEProperties.KEY_TYPE);
	//	String type = GEType.PHASED_FILTER;
		
		if (GEType.PIPELINE == type)
		{
			node = new GEPipeline(name);
		}
		else if (GEType.PHASED_FILTER == type)
		{
			node = new GEPhasedFilter(name);
		}
		else
		{
			System.out.println("Invalid type for GEStreamNode vertex");
			return null;
		}
		
		node.setOutputTape(properties.getProperty(GEProperties.KEY_OUTPUT_TAPE));
		node.setInputTape(properties.getProperty(GEProperties.KEY_INPUT_TAPE));
		
		GEStreamNode parentNode = graphpad.getCurrentDocument().getGraphStructure().
			 getContainerNodeFromName(properties.getProperty(GEProperties.KEY_PARENT));
		node.setEncapsulatingNode(parentNode);
		
		
		Map map = GraphConstants.createMap();
		GraphConstants.setBounds(map, bounds);
		GraphConstants.setBorderColor(map, Color.BLACK);
		GraphConstants.setAutoSize(map, true);		
		
		node.setAttributes(map);				
		DefaultPort port = new DefaultPort();
		node.add(port);
		node.setPort(port);
		
		glc.insert(new Object[] {node}, null, null, null, null);
		ggg.getModel().insert(new Object[] {node}, null, null, null, null);
		glc.setVisible(node, false);
		glc.setVisible(node, true);
		
		return null;
	}
*/




	//
	// Cell Creation
	//

	public Object addVertex(
		int type,
		Object userObject,
		Rectangle bounds,
		boolean autosize,
		Color border) {
		Map viewMap = new Hashtable();
		Map map;
		GraphModelProvider gmp = graphpad.getCurrentGraphModelProvider();
		GraphModel model = graphpad.getCurrentGraph().getModel();

		// Create Vertex
		Object obj = (userObject instanceof String) ? userObject : "";
		//DefaultGraphCell cell;
		/*
		if (userObject instanceof ImageIcon)
			cell = new ImageCell(obj);
		else if (userObject.toString().equals("Text"))
			cell = new TextCell("Type here");
		else
			cell = new DefaultGraphCell(obj);
		*/
		map = GraphConstants.createMap();
		GraphConstants.setBounds(map, bounds);
		GraphConstants.setOpaque(map, false);
		if (border != null)
			GraphConstants.setBorderColor(map, border);
		String fontName = Translator.getString("FontName");
		try {
			int fontSize = Integer.parseInt(Translator.getString("FontSize"));
			int fontStyle = Integer.parseInt(Translator.getString("FontStyle"));
			GraphConstants.setFont(
				map,
				new Font(fontName, fontStyle, fontSize));
		} catch (Exception e) {
			// handle error
		}

		if (autosize)
			GraphConstants.setAutoSize(map, true);
		List toInsert = new LinkedList();

		Object cell;
		switch (type) {
			case GraphModelProvider.CELL_VERTEX_IMAGE :
				cell =
					gmp.createCell(
						model,
						GraphModelProvider.CELL_VERTEX_IMAGE,
						new GPUserObject(""),
						map);
				break;
			case GraphModelProvider.CELL_VERTEX_TEXT :
				cell =
					gmp.createCell(
						model,
						GraphModelProvider.CELL_VERTEX_TEXT,
						new GPUserObject(userObject.toString()),
						map);
				break;
			default :
				cell =
					gmp.createCell(
						model,
						GraphModelProvider.CELL_VERTEX_DEFAULT,
						new GPUserObject(userObject.toString()),
						map);
				break;
		}
		viewMap.put(cell, map);
		toInsert.add(cell);

		// Create Ports
		int u = GraphConstants.PERCENT;
		//DefaultPort port;
		Object port;

		// Floating Center Port (Child 0 is Default)
		// port = new DefaultPort("Center");
		// cell.add(port);
		port = gmp.createCell(model, GraphModelProvider.CELL_PORT_DEFAULT, "Center", null);
		gmp.addPort(cell, port);
		toInsert.add(port);

		if (userObject instanceof ImageIcon) {
			GraphConstants.setIcon(map, (ImageIcon) userObject);

			// Single non-floating central-port
			map = GraphConstants.createMap();
			GraphConstants.setOffset(
				map,
				new Point((int) (u / 2), (int) (u / 2)));
			viewMap.put(port, map);
			toInsert.add(port);
		} else {
			// Top Left
			//port = new DefaultPort("Topleft");
			//cell.add(port);
			map = GraphConstants.createMap();
			GraphConstants.setOffset(map, new Point(0, 0));
			port = gmp.createCell(model, GraphModelProvider.CELL_PORT_DEFAULT, "Topleft", map);
			gmp.addPort(cell, port);
			viewMap.put(port, map);
			toInsert.add(port);

			// Top Center
			//port = new DefaultPort("Topcenter");
			//cell.add(port);
			map = GraphConstants.createMap();
			GraphConstants.setOffset(map, new Point((int) (u / 2), 0));
			port =
				gmp.createCell(model, GraphModelProvider.CELL_PORT_DEFAULT, "Topcenter", map);
			gmp.addPort(cell, port);
			viewMap.put(port, map);
			toInsert.add(port);

			// Top Right
			//port = new DefaultPort("Topright");
			//cell.add(port);
			map = GraphConstants.createMap();
			GraphConstants.setOffset(map, new Point(u, 0));
			port =
				gmp.createCell(model, GraphModelProvider.CELL_PORT_DEFAULT, "Topright", map);
			gmp.addPort(cell, port);
			viewMap.put(port, map);
			toInsert.add(port);

			// Top Center
			//port = new DefaultPort("Middleleft");
			//cell.add(port);
			map = GraphConstants.createMap();
			GraphConstants.setOffset(map, new Point(0, (int) (u / 2)));
			port =
				gmp.createCell(model, GraphModelProvider.CELL_PORT_DEFAULT, "Middleleft", map);
			gmp.addPort(cell, port);
			viewMap.put(port, map);
			toInsert.add(port);

			// Middle Right
			//port = new DefaultPort("Middleright");
			//cell.add(port);
			map = GraphConstants.createMap();
			GraphConstants.setOffset(map, new Point(u, (int) (u / 2)));
			port =
				gmp.createCell(
					model,
					GraphModelProvider.CELL_PORT_DEFAULT,
					"Middleright",
					map);
			gmp.addPort(cell, port);
			viewMap.put(port, map);
			toInsert.add(port);

			// Bottom Left
			//port = new DefaultPort("Bottomleft");
			//cell.add(port);
			map = GraphConstants.createMap();
			GraphConstants.setOffset(map, new Point(0, u));
			port =
				gmp.createCell(model, GraphModelProvider.CELL_PORT_DEFAULT, "Bottomleft", map);
			gmp.addPort(cell, port);
			viewMap.put(port, map);
			toInsert.add(port);

			// Bottom Center
			//port = new DefaultPort("Bottomcenter");
			//cell.add(port);
			map = GraphConstants.createMap();
			GraphConstants.setOffset(map, new Point((int) (u / 2), u));
			port =
				gmp.createCell(
					model,
					GraphModelProvider.CELL_PORT_DEFAULT,
					"Bottomcenter",
					map);
			gmp.addPort(cell, port);
			viewMap.put(port, map);
			toInsert.add(port);

			// Bottom Right
			//port = new DefaultPort("Bottomright");
			//cell.add(port);
			map = GraphConstants.createMap();
			GraphConstants.setOffset(map, new Point(u, u));
			port =
				gmp.createCell(
					model,
					GraphModelProvider.CELL_PORT_DEFAULT,
					"Bottomright",
					map);
			gmp.addPort(cell, port);
			viewMap.put(port, map);
			toInsert.add(port);
		}

		graphpad.getCurrentGraph().getModel().insert(
			toInsert.toArray(),
			viewMap,
			null,
			null,
			null);
		return cell;
	}

	// Use net.GraphCellFactory here!
	public void addEllipse(Object userObject, Rectangle bounds) {
		Map viewMap = new Hashtable();
		Map map;
		GraphModelProvider gmp = graphpad.getCurrentGraphModelProvider();
		GraphModel model = graphpad.getCurrentGraph().getModel();

		// Create Vertex
		// EllipseCell cell = new EllipseCell(userObject);
		map = GraphConstants.createMap();
		GraphConstants.setBounds(map, bounds);
		GraphConstants.setOpaque(map, false);
		GraphConstants.setBorderColor(map, Color.black);
		String fontName = Translator.getString("FontName");
		try {
			int fontSize = Integer.parseInt(Translator.getString("FontSize"));
			int fontStyle = Integer.parseInt(Translator.getString("FontStyle"));
			GraphConstants.setFont(
				map,
				new Font(fontName, fontStyle, fontSize));
		} catch (Exception e) {
			// handle error
		}
		List toInsert = new LinkedList();

		Object cell =
			gmp.createCell(
				model,
				GraphModelProvider.CELL_VERTEX_ELLIPSE,
				new GPUserObject(userObject.toString()),
				map);

		viewMap.put(cell, map);
		toInsert.add(cell);

		// Create Ports
		int u = GraphConstants.PERCENT;
		Object port;

		// Floating Center Port
		port =
			gmp.createCell(
				model,
				GraphModelProvider.CELL_PORT_DEFAULT,
				"Center",
				null);
		gmp.addPort(cell, port);
		//port = new DefaultPort("Center");
		//cell.add(port);
		toInsert.add(port);

		// Top Left
		//port = new DefaultPort("Topleft");
		//cell.add(port);
		map = GraphConstants.createMap();
		GraphConstants.setOffset(
			map,
			new Point((int) (u * 0.15), (int) (u * 0.15)));
		port =
			gmp.createCell(
				model,
				GraphModelProvider.CELL_PORT_DEFAULT,
				"Topleft",
				map);
		gmp.addPort(cell, port);
		viewMap.put(port, map);
		toInsert.add(port);

		// Top Center
		// port = new DefaultPort("Topcenter");
		//cell.add(port);
		map = GraphConstants.createMap();
		GraphConstants.setOffset(map, new Point((int) (u / 2), 0));
		port =
			gmp.createCell(
				model,
				GraphModelProvider.CELL_PORT_DEFAULT,
				"Topcenter",
				map);
		gmp.addPort(cell, port);
		viewMap.put(port, map);
		toInsert.add(port);

		// Top Right
		// port = new DefaultPort("Topright");
		// cell.add(port);
		map = GraphConstants.createMap();
		GraphConstants.setOffset(
			map,
			new Point((int) (u * 0.85), (int) (u * 0.15)));
		port =
			gmp.createCell(
				model,
				GraphModelProvider.CELL_PORT_DEFAULT,
				"Topright",
				map);
		gmp.addPort(cell, port);
		viewMap.put(port, map);
		toInsert.add(port);

		// Top Center
		//port = new DefaultPort("Middleleft");
		//cell.add(port);
		map = GraphConstants.createMap();
		GraphConstants.setOffset(map, new Point(0, (int) (u / 2)));
		port =
			gmp.createCell(
				model,
				GraphModelProvider.CELL_PORT_DEFAULT,
				"Middleleft",
				map);
		gmp.addPort(cell, port);
		viewMap.put(port, map);
		toInsert.add(port);

		// Middle Right
		//port = new DefaultPort("Middleright");
		//cell.add(port);
		map = GraphConstants.createMap();
		GraphConstants.setOffset(map, new Point(u, (int) (u / 2)));
		port =
			gmp.createCell(
				model,
				GraphModelProvider.CELL_PORT_DEFAULT,
				"Middleright",
				map);
		gmp.addPort(cell, port);
		viewMap.put(port, map);
		toInsert.add(port);

		// Bottom Left
		//port = new DefaultPort("Bottomleft");
		//cell.add(port);
		map = GraphConstants.createMap();
		GraphConstants.setOffset(
			map,
			new Point((int) (u * 0.15), (int) (u * 0.85)));
		port =
			gmp.createCell(
				model,
				GraphModelProvider.CELL_PORT_DEFAULT,
				"Bottomleft",
				map);
		gmp.addPort(cell, port);
		viewMap.put(port, map);
		toInsert.add(port);

		// Bottom Center
		//port = new DefaultPort("Bottomcenter");
		//cell.add(port);
		map = GraphConstants.createMap();
		GraphConstants.setOffset(map, new Point((int) (u / 2), u));
		port =
			gmp.createCell(
				model,
				GraphModelProvider.CELL_PORT_DEFAULT,
				"Bottomcenter",
				map);
		gmp.addPort(cell, port);
		viewMap.put(port, map);
		toInsert.add(port);

		// Bottom Right
		//port = new DefaultPort("Bottomright");
		//cell.add(port);
		map = GraphConstants.createMap();
		GraphConstants.setOffset(
			map,
			new Point((int) (u * 0.85), (int) (u * 0.85)));
		port =
			gmp.createCell(
				model,
				GraphModelProvider.CELL_PORT_DEFAULT,
				"Bottomright",
				map);
		gmp.addPort(cell, port);
		viewMap.put(port, map);
		toInsert.add(port);

		graphpad.getCurrentGraph().getModel().insert(
			toInsert.toArray(),
			viewMap,
			null,
			null,
			null);
	}

	public void writeTemplateCodeToFile(String template)
	{
		try 
		{
			IFile ifile = graphpad.getCurrentDocument().getIFile();
			if (ifile != null)
			{
				ByteArrayInputStream bais = new ByteArrayInputStream(template.getBytes());
				ifile.appendContents(bais, false, false, null);
				ifile.getParent().refreshLocal(IResource.DEPTH_ONE, null);
			}
	
		} 
		catch (Exception exception) 
		{
			exception.printStackTrace();
		}		
	}



	/**
	 * Returns the buttonFilter.
	 * @return JToggleButton
	 */
	public JToggleButton getButtonFilter() {
		return buttonFilter;
	}

	/**
	 * Returns the buttonEdge.
	 * @return JToggleButton
	 */
	public JToggleButton getButtonEdge() {
		return buttonEdge;
	}

	/**
	 * Returns the buttonJoiner.
	 * @return JToggleButton
	 */
	public JToggleButton getButtonJoiner() {
		return buttonJoiner;
	}

	/**
	 * Returns the buttonSplitJoin.
	 * @return JToggleButton
	 */
	public JToggleButton getButtonSplitJoin() {
		return buttonSplitJoin;
	}

	/**
	 * Returns the buttonFeedbackLoop.
	 * @return JToggleButton
	 */
	public JToggleButton getButtonFeedbackLoop() {
		return buttonFeedbackLoop;
	}
	
	/**
	 * Returns the buttonNode.
	 * @return JToggleButton
	 */
	public JToggleButton getButtonNode() {
		return buttonNode;
	}

	/**
	 * Returns the buttonRectangle.
	 * @return JToggleButton
	 */
	public JToggleButton getButtonRectangle() {
		return buttonRectangle;
	}

	/**
	 * Returns the buttonSelect.
	 * @return JToggleButton
	 */
	public JToggleButton getButtonSelect() {
		return buttonSelect;
	}

	/**
	 * Returns the buttonSplitter.
	 * @return JToggleButton
	 */
	public JToggleButton getButtonSplitter() {
		return buttonSplitter;
	}

	/**
	 * Returns the buttonZoomArea.
	 * @return JToggleButton
	 */
	public JToggleButton getButtonZoomArea() {
		return buttonZoomArea;
	}

}
