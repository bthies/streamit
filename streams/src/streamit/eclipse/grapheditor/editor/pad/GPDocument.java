package streamit.eclipse.grapheditor.editor.pad;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.ToolTipManager;

import org.eclipse.core.resources.IFile;
import org.jgraph.JGraph;
import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.GraphUndoManager;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.actions.GraphCreateTemplate;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import streamit.eclipse.grapheditor.editor.utils.Utilities;
import streamit.eclipse.grapheditor.editor.utils.gui.GPSplitPane;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * A Document is a panel with a splitpane
 * the splitpane shows a library resource
 * and a
 *
 * @author sven.luzar
 *
 */
public class GPDocument
	extends JPanel
	implements
		GraphSelectionListener,
		ComponentListener,
		Printable,
		GraphModelListener,
		PropertyChangeListener,
		Observer {

	/**
	 */
	protected boolean enableTooltips;

	/** Filename for the current document.
	 * Null if never saved or opened. *
	 *
	 */
	protected URL file;

	/** A reference to the top level
	 *  component
	 */
	protected GPGraphpad graphpad;

	/** Splitpane between the
	 * libraries and the graph
	 */
	protected GPSplitPane splitPane;

	/** The left site of this document
	 *  Shows the libraries
	 *
	 */
	protected GPLibraryPanel libraryPanel;

	/** Container for the graph so that you
	 *  can scroll over the graph
	 */
	protected JScrollPane scrollPane;

	/** The joint graph for this document
	 */
	protected GPGraph graph;

	/** The overview Dialog for this document.
	 *  Can be <tt>null</tt>.
	 *
	 */
	protected JDialog overviewDialog;

	/** The graphUndoManager manager for the joint graph.
	 *
	 *  @see #graph
	 */
	protected GraphUndoManager graphUndoManager;


	/** True if this documents graph model
	 *  was modified since last save. */
	protected boolean modified = false;

	/** true if the library expand is expanded
	 *  default is true
	 */
	protected static boolean libraryExpanded = true;

	/** Action used for fitting the size
	 */
	protected Action fitAction;

	/** contains the find pattern for this document
	 */
	protected String findPattern;

	/** contains the last found object
	 */
	protected Object lastFound;

	/** a reference to the internal Frame
	 */
	protected GPInternalFrame internalFrame;

	/** a reference to the graph model provider for
	 *  this document
	 */
	protected GraphModelProvider graphModelProvider;

	


	/**
	 * The graphStructure corresponding to the current graph
	 * being displayed in the panel. 
	 * added by jcarlos
	 */
	protected GraphStructure graphStruct;
	
	/**
	 * The IFile that corresponds to this GPDocument.
	 */
	protected IFile ifile;
	
	protected GEStreamNode clonedNode = null;
	
	/**
	 * The hierarchy panel of the document.
	 */
	public TreePanel treePanel;
	
	/**
	 * Boolean that specifies wheter or not the containers in the 
	 * document are visible
	 */
	public boolean containersInvisible = true; 
	
	

	/** Static initializer.
	 *  Initializes some static variables.
	 */
	static {
		libraryExpanded =
			new Boolean(Translator.getString("LibraryExpanded")).booleanValue();
	}

	/**
	 * Constructor for GPDocument.
	 */
	public GPDocument(
		GPGraphpad graphpad,
		URL file,
		GraphModelProvider graphModelProvider,
		GPGraph gpGraph,
		GraphModel model,
		GraphUndoManager undo) {
		super(true);

		this.file = file;
		this.graphpad = graphpad;
		this.graphModelProvider = graphModelProvider;


		this.graphUndoManager = undo == null ? createGraphUndoManager() : undo;

		setBorder(BorderFactory.createEtchedBorder());
		setLayout(new BorderLayout());

		// create the graph
		graph = gpGraph;
	
		registerListeners(graph);


	
		Component comp = this.createCenterComponent();
		this.add(comp);

		addComponentListener(this);

		setEnableTooltips(false);
		update();
	}
	
	/**
	 * Constructor that sets the GraphStructure field. 
	 * added by jcarlos
	 */
	public GPDocument(
		GPGraphpad graphpad,
		URL file,
		GraphModelProvider graphModelProvider,
		GPGraph gpGraph,
		GraphModel model,
		GraphStructure graphStruct,
		GraphUndoManager undo) {
		super(true);

		this.graphStruct = graphStruct;
		
		this.file = file;
		this.graphpad = graphpad;
		this.graphModelProvider = graphModelProvider;
		this.graphStruct = graphStruct;

		this.graphUndoManager = undo == null ? createGraphUndoManager() : undo;

		setBorder(BorderFactory.createEtchedBorder());
		setLayout(new BorderLayout());

		// create the graph
		graph = gpGraph;
		
		registerListeners(graph);

	
		Component comp = this.createCenterComponent();
		this.add(comp);

		addComponentListener(this);

		setEnableTooltips(false);
		update();
	}
	
	
	

	/**
	 * Returns the filename.
	 * @return String
	 */
	public URL getFilename() {
		return file;
	}

	/**
	 * Sets the filename.
	 * @param filename The filename to set
	 */
	public void setFilename(URL filename) {
		this.file = filename;
		updateFrameTitle();
	}

	/**
	 * Create the center component of this panel.
	 * This creates a scroll-
	 * pane for the current graph variable and stores the scrollpane
	 * in the scrollPane variable.
	 */
	protected Component createCenterComponent() {
		Component mainPane = createScrollPane();

	//	libraryPanel = createLibrary();
		treePanel = new TreePanel();

		
		JPanel overviewPane = GPOverviewPanel.createOverviewPanel(getGraph(), this);
		JSplitPane librarySplit = new GPSplitPane(
			JSplitPane.VERTICAL_SPLIT,
			overviewPane,
			treePanel

		);
		librarySplit.setName("DocumentLibrary");
		//librarySplit.setDividerLocation(80);
		
		splitPane =
			new GPSplitPane(
				GPSplitPane.HORIZONTAL_SPLIT,
				librarySplit,
				mainPane);
		splitPane.setName("DocumentMain");
		splitPane.setOneTouchExpandable(true);
		/*
		if (!libraryExpanded)
			splitPane.setDividerLocation(0.0d);
			*/

		return splitPane;
	}

	protected Component createScrollPane() {
		scrollPane = new JScrollPane(graph);
		JViewport port = scrollPane.getViewport();
		try {
			String vpFlag = Translator.getString("ViewportBackingStore");
			Boolean bs = new Boolean(vpFlag);
			if (bs.booleanValue()) {
				port.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
			} else {
				port.setScrollMode(JViewport.BLIT_SCROLL_MODE);
			}
		} catch (MissingResourceException mre) {
			// just use the viewport default
		}
		return scrollPane;
	}

	protected GPLibraryPanel createLibrary() {
		return new GPLibraryPanel(
			Translator.getString("LibraryName"),
			Utilities.tokenize(Translator.getString("LoadLibraries")),
			new Integer(Translator.getString("EntrySize")).intValue());
	}

	protected GraphUndoManager createGraphUndoManager() {
		return new GraphUndoManager();
	}

	/**
	 * Return the GraphStructure corresponding to this GPDocument.
	 * @return graphStruct
	 */
	public GraphStructure getGraphStructure()
	{
		return this.graphStruct;
	}
	
	/**
	 * Return the IFile that belongs to this GPDocument.
	 * @return
	 */
	public IFile getIFile()
	{
		return this.ifile;
	}
	
	/**
	 * Set the IFile of the GPDocument.
	 * @param ifile
	 */
	public void setIFile(IFile ifile)
	{
		this.ifile = ifile;
	}
	
	/**
	 * Determine wheter or not the containers in this 
	 * document are invisible.
	 * @return True if the containers are invisible,; false, otherwise.
	 */
	public boolean areContainersInvisible()
	{
		return this.containersInvisible;
	}
	
	/**
	 * Set the boolean that specifies wheter or not the containers are
	 * invisible
	 * @param vis boolean
	 */
	public void setContainersInvisible(boolean vis)
	{
		this.containersInvisible =  vis;
	}
	
	
	/** 
	 * Get the node that has been cloned (or null if there is no clonedNode)
	 * @return GEStreamNode The node that has been cloned. 
	 */
	public GEStreamNode getClonedNode()
	{
		return this.clonedNode;
	}

	/**
	 * Set the node that has been cloned
	 * @param node GEStreamNode node that has been cloned. 
	 */
	public void setClonedNode(GEStreamNode node)
	{
		this.clonedNode = node;
	}
	
	/**
	 * Fetch the editor contained in this panel
	 */
	public GPGraph getGraph() {
		return graph;
	}

	/** Returns the model of the graph
	 */
	public GraphModel getModel() {
		return graph.getModel();
	}

	/** returns the GPGraph UI
	 */
	public GPGraphUI getGraphUI() {
		return (GPGraphUI) graph.getUI();
	}

	/** Returns the view from the current graph
	 *
	 */
	public GraphLayoutCache getGraphLayoutCache() {
		return graph.getGraphLayoutCache();
	}

	/* Add this documents listeners to the specified graph. */
	protected void registerListeners(JGraph graph) {
		graph.getSelectionModel().addGraphSelectionListener(this);
		graph.getModel().addGraphModelListener(this);
		graph.getGraphLayoutCache().addObserver(this);
		graph.addPropertyChangeListener(this);
	}

	/* Remove this documents listeners from the specified graph. */
	protected void unregisterListeners(JGraph graph) {
		graph.getSelectionModel().removeGraphSelectionListener(this);
		graph.getModel().removeGraphModelListener(this);
		graph.removePropertyChangeListener(this);
	}

	public void setModified(boolean modified) {
		this.modified = modified;
		updateFrameTitle();
		graphpad.update();
	}

	/* Return the title of this document as a string. */
	protected String getDocumentTitle() {
		String s =
			(file != null) ? file.toString() : Translator.getString("NewGraph");
		if (modified)
			s = s + " *";
		return Translator.getString("Title") + " - " + s;
	}

	/* Return the status of this document as a string. */
	protected String getDocumentStatus() {
		String s = null;
		int n = graph.getSelectionCount();
		if (n > 0)
			s = n + " " + Translator.getString("Selected");
		else {
			int c = graph.getModel().getRootCount();
			if (c == 0) {
				s = Translator.getString("Empty");
			} else {
				s = c + " ";
				if (c > 1)
					s += Translator.getString("Cells");
				else
					s += Translator.getString("Cell");
				c = GPGraphpad.getGraphTools().getComponentCount(graph);
				s = s + " / " + c + " ";
				if (c > 1)
					s += Translator.getString("Components");
				else
					s += Translator.getString("Component");
			}
		}
		return s;
	}

	/* Return the scale of this document as a string. */
	protected String getDocumentScale() {
		return Integer.toString((int) (graph.getScale() * 100)) + "%";
	}

	/* Sets the attributes of the selected cells. */
	public void setSelectionAttributes(Map map) {
		Object[] cells =
			DefaultGraphModel
				.getDescendants(graph.getModel(), graph.getSelectionCells())
				.toArray();
		//Filter ports out
		java.util.List list = new ArrayList();
		for (int i = 0; i < cells.length; i++)
			if (!graph.isPort(cells[i]))
				list.add(cells[i]);
		cells = list.toArray();
		map = GraphConstants.cloneMap(map);
		map.remove(GraphConstants.BOUNDS);
		map.remove(GraphConstants.POINTS);

		Map nested = new Hashtable();
		for (int i = 0; i < cells.length; i++)
			nested.put(cells[i], GraphConstants.cloneMap(map));
		graph.getGraphLayoutCache().edit(nested, null, null, null);
	}


	//-----------------------------------------------------------------
	// Component Listener
	//-----------------------------------------------------------------
	public void setResizeAction(AbstractAction e) {
		fitAction = e;
	}

	public void componentHidden(ComponentEvent e) {
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentResized(ComponentEvent e) {
		if (fitAction != null)
			fitAction.actionPerformed(null);
	}

	public void componentShown(ComponentEvent e) {
		componentResized(e);
	}

	public void setScale(double scale) {
		scale = Math.max(Math.min(scale, 16), .01);
		graph.setScale(scale);
		componentResized(null);
	}

	//----------------------------------------------------------------------
	// Printable
	//----------------------------------------------------------------------

	/** not from Printable interface, but related
	 */
	public void updatePageFormat() {
		PageFormat f = graph.getPageFormat();
		if (graph.isPageVisible()) {
			int w = (int) (f.getWidth());
			int h = (int) (f.getHeight());
			graph.setMinimumSize(new Dimension(w + 5, h + 5));
		} else
			graph.setMinimumSize(null);
		invalidate();
		// Execute fitAction...
		componentResized(null);
		graph.repaint();
	}

	public int print(Graphics g, PageFormat pF, int page) {
		int pw = (int) pF.getImageableWidth();
		int ph = (int) pF.getImageableHeight();
		int cols = (int) (graph.getWidth() / pw) + 1;
		int rows = (int) (graph.getHeight() / ph) + 1;
		int pageCount = cols * rows;
		if (page >= pageCount)
			return NO_SUCH_PAGE;
		int col = page % cols;
		int row = page % rows;
		g.translate(-col*pw, -row*ph);
		g.setClip(col*pw, row*ph, pw, ph);
		graph.paint(g);
		g.translate(col*pw, row*ph);
		return PAGE_EXISTS;
	}

	//
	// Listeners
	//

	// PropertyChangeListener
	public void propertyChange(PropertyChangeEvent evt) {
		if (graphpad != null)
			update();
	}

	// GraphSelectionListener
	public void valueChanged(GraphSelectionEvent e) {
		if (!graph.isSelectionEmpty())
		{
		}
		update();
	}

	// View Observer
	public void update(Observable obs, Object arg) {
		modified = true;
		
		update();
	}

	// GraphModelListener
	public void graphChanged(GraphModelEvent e) {
		modified = true;
		
		update();
		//System.out.println("Change:\n"+buttonEdge.getChange().getStoredAttributeMap());
	}

	protected void update() {
		updateFrameTitle();
		graphpad.update();
		graphpad.getStatusBar().setMessage(this.getDocumentStatus());
		graphpad.getStatusBar().setScale(this.getDocumentScale());
	}

	/**
	 * Returns the graphUndoManager.
	 * @return GraphUndoManager
	 */
	public GraphUndoManager getGraphUndoManager() {
		return graphUndoManager;
	}

	/**
	 * Sets the graphUndoManager.
	 * @param graphUndoManager The graphUndoManager to set
	 */
	public void setGraphUndoManager(GraphUndoManager graphUndoManager) {
		this.graphUndoManager = graphUndoManager;
	}

	/** Resets the Graph undo manager
	 */
	public void resetGraphUndoManager() {
		graphUndoManager.discardAllEdits();
	}

	/**
	 * Returns the graphpad.
	 * @return GPGraphpad
	 */
	public GPGraphpad getGraphpad() {
		return graphpad;
	}

	/**
	 * Sets the graphpad.
	 * @param graphpad The graphpad to set
	 */
	public void setGraphpad(GPGraphpad graphpad) {
		this.graphpad = graphpad;
	}

	

	/**
	 * Returns true if the user really wants to close.
	 * Gives chance to save work.
	 */
	public boolean close(boolean showConfirmDialog) {
		// set default to save on close
		int r = JOptionPane.YES_OPTION;

		if (modified){
			if (showConfirmDialog)
				r = JOptionPane.showConfirmDialog(
					graphpad.getFrame(),
					Translator.getString("CreateTemplateDialog"),
					Translator.getString("Title"),
					JOptionPane.YES_NO_CANCEL_OPTION);

            // if yes, then save and close
			if (r == JOptionPane.YES_OPTION) {
				graphpad
					.getCurrentActionMap()
					.get(Utilities.getClassNameWithoutPackage(GraphCreateTemplate.class))
					.actionPerformed(null);
				return true;
			}
			// if no, then don't save and just close
			else if (r == JOptionPane.NO_OPTION) {
				return true;
			}
			// all other conditions (cancel and dialog's 'X' button)
			// don't save and don't close
			else
				return false;

		}
		else
			// if not modified just close
			return true;
	}


	public TreePanel getTreePanel() {
		return treePanel;
	}


	/** This will change the source of the actionevent to graph.
	 * */
	protected class EventRedirector implements ActionListener {

		protected Action action;

		public EventRedirector(Action a) {
			this.action = a;
		}

		public void actionPerformed(ActionEvent e) {
			JComponent source = graph;
			if (libraryPanel.hasFocus())
				source = libraryPanel;
			e =
				new ActionEvent(
					source,
					e.getID(),
					e.getActionCommand(),
					e.getModifiers());
			action.actionPerformed(e);
		}

	}

	/**
	 * Returns the findPattern.
	 * @return String
	 */
	public String getFindPattern() {
		return findPattern;
	}

	/**
	 * Sets the findPattern.
	 * @param findPattern The findPattern to set
	 */
	public void setFindPattern(String findPattern) {
		this.findPattern = findPattern;
	}

	/**
	 * Returns the lastFound.
	 * @return Object
	 */
	public Object getLastFound() {
		return lastFound;
	}

	/**
	 * Sets the lastFound.
	 * @param lastFound The lastFound to set
	 */
	public void setLastFound(Object lastFound) {
		this.lastFound = lastFound;
	}

	/**
	 * Returns the overviewDialog.
	 * @return JDialog
	 */
	public JDialog getOverviewDialog() {
		return overviewDialog;
	}

	/**
	 * Sets the overviewDialog.
	 * @param overviewDialog The overviewDialog to set
	 */
	public void setOverviewDialog(JDialog overviewDialog) {
		this.overviewDialog = overviewDialog;
	}

	/**
	 * Returns the splitPane.
	 * @return JSplitPane
	 */
	public GPSplitPane getSplitPane() {
		return splitPane;
	}

	/**
	 * Sets the splitPane.
	 * @param splitPane The splitPane to set
	 */
	public void setSplitPane(GPSplitPane splitPane) {
		this.splitPane = splitPane;
	}

	/**
	 * Returns the scrollPane.
	 * @return JScrollPane
	 */
	public JScrollPane getScrollPane() {
		return scrollPane;
	}

	/**
	 * Sets the scrollPane.
	 * @param scrollPane The scrollPane to set
	 */
	public void setScrollPane(JScrollPane scrollPane) {
		this.scrollPane = scrollPane;
	}


	/**
	 * Returns the enableTooltips.
	 * @return boolean
	 */
	public boolean isEnableTooltips() {
		return enableTooltips;
	}

	/**
	 * Sets the enableTooltips.
	 * @param enableTooltips The enableTooltips to set
	 */
	public void setEnableTooltips(boolean enableTooltips) {
		this.enableTooltips = enableTooltips;

		if (this.enableTooltips)
			ToolTipManager.sharedInstance().registerComponent(graph);
		else
			ToolTipManager.sharedInstance().unregisterComponent(graph);
	}

	/**
	 * Returns the internalFrame.
	 * @return GPInternalFrame
	 */
	public GPInternalFrame getInternalFrame() {
		return internalFrame;
	}

	/**
	 * Sets the internalFrame.
	 * @param internalFrame The internalFrame to set
	 */
	protected void setInternalFrame(GPInternalFrame internalFrame) {
		this.internalFrame = internalFrame;
	}

	protected void updateFrameTitle() {
		if (this.internalFrame != null) {
			this.internalFrame.setTitle(getFrameTitle());
		}

	}

	public String getFrameTitle() {
		return (
			this.file == null
				? Translator.getString("NewGraph")
				: file.toString())
			+ (modified ? "*" : "");
	}
	/**
	 * Returns the graphModelProvider.
	 * @return GraphModelProvider
	 */
	public GraphModelProvider getGraphModelProvider() {
		return graphModelProvider;
	}

	/**
	 * Returns the networkModel.
	 * @return GraphNetworkModel
	 */


	/* Sets the attributes of the selected cells. */
/*	Removed by jcarlos
	public void setFontSizeForSelection(float size) {
		Object[] cells =
			DefaultGraphModel
				.getDescendants(graph.getModel(), graph.getSelectionCells())
				.toArray();
		//Filter ports out
		java.util.List list = new ArrayList();
		for (int i = 0; i < cells.length; i++)
			if (!graph.isPort(cells[i]))
				list.add(cells[i]);
		cells = list.toArray();

		Map nested = new Hashtable();
		for (int i = 0; i < cells.length; i++) {
			CellView view = graph.getGraphLayoutCache().getMapping(cells[i], false);
			if (view != null) {
				Font font = GraphConstants.getFont(view.getAllAttributes());
				Map attr = GraphConstants.createMap();
				GraphConstants.setFont(attr, font.deriveFont(size));
				nested.put(cells[i], attr);
			}
		}
		graph.getGraphLayoutCache().edit(nested, null, null, null);
	}
*/	


	/* Sets the attributes of the selected cells. */
/* Removed by jcarlos
	public void setFontStyleForSelection(int style) {
		Object[] cells =
			DefaultGraphModel
				.getDescendants(graph.getModel(), graph.getSelectionCells())
				.toArray();
		//Filter ports out
		java.util.List list = new ArrayList();
		for (int i = 0; i < cells.length; i++)
			if (!graph.isPort(cells[i]))
				list.add(cells[i]);
		cells = list.toArray();

		Map nested = new Hashtable();
		for (int i = 0; i < cells.length; i++) {
			CellView view = graph.getGraphLayoutCache().getMapping(cells[i], false);
			if (view != null) {
				Font font = GraphConstants.getFont(view.getAllAttributes());
				Map attr = GraphConstants.createMap();
				GraphConstants.setFont(attr, font.deriveFont(style));
				nested.put(cells[i], attr);
			}
		}
		graph.getGraphLayoutCache().edit(nested, null, null, null);
	}
*/
	/* Sets the attributes of the selected cells. */
/*	Removed by jcarlos
	public void setFontNameForSelection(String name) {
		Object[] cells =
			DefaultGraphModel
				.getDescendants(graph.getModel(), graph.getSelectionCells())
				.toArray();
		//Filter ports out
		java.util.List list = new ArrayList();
		for (int i = 0; i < cells.length; i++)
			if (!graph.isPort(cells[i]))
				list.add(cells[i]);
		cells = list.toArray();

		Map nested = new Hashtable();
		for (int i = 0; i < cells.length; i++) {
			CellView view = graph.getGraphLayoutCache().getMapping(cells[i], false);
			if (view != null) {
				Font font = GraphConstants.getFont(view.getAllAttributes());
				Map attr = GraphConstants.createMap();
				GraphConstants.setFont(attr, new Font(name, font.getStyle(), font.getSize()));
				nested.put(cells[i], attr);
			}
		}
		graph.getGraphLayoutCache().edit(nested, null, null, null);
	}
*/
}
