/*
 * @(#)GPGraphpad.java	1.2 11/11/02
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

package streamit.eclipse.grapheditor.editor;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ContainerListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.eclipse.core.resources.IFile;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.GraphUndoManager;

import streamit.eclipse.grapheditor.editor.pad.DefaultGraphModelFileFormatStreamIt;
import streamit.eclipse.grapheditor.editor.pad.DefaultGraphModelProvider;
import streamit.eclipse.grapheditor.editor.pad.GPBarFactory;
import streamit.eclipse.grapheditor.editor.pad.GPConfiguration;
import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.editor.pad.GPGraph;
import streamit.eclipse.grapheditor.editor.pad.GPGraphTools;
import streamit.eclipse.grapheditor.editor.pad.GPInternalFrame;
import streamit.eclipse.grapheditor.editor.pad.GPMarqueeHandler;
import streamit.eclipse.grapheditor.editor.pad.GPStatusBar;
import streamit.eclipse.grapheditor.editor.pad.GraphModelFileFormat;
import streamit.eclipse.grapheditor.editor.pad.GraphModelProvider;
import streamit.eclipse.grapheditor.editor.pad.GraphModelProviderRegistry;
import streamit.eclipse.grapheditor.editor.pad.actions.*;
import streamit.eclipse.grapheditor.editor.pad.resources.ImageLoader;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import streamit.eclipse.grapheditor.editor.utils.Utilities;
import streamit.eclipse.grapheditor.editor.utils.gui.GPFrame;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 *
 * @author  Gaudenz Alder
 * @author  Sven Luzar
 * @version 1.3.2 Actions moved to an own package
 * @version 1.0 1/1/02
 */
public class GPGraphpad extends JPanel {

	/** Pointer to enclosing JGraphpad applet, if any
	 */
	public transient JGraphpad applet;

	/** Application Icon. From resource file.
	 */
	public static ImageIcon applicationIcon;

	/** Application Icon. From resource file.
	 */
	protected static ImageIcon logoIcon;

	/** Application Title. From resource file.
	 */
	protected static String appTitle;

	/** Default entry size
	 */
	protected static int entrySize = 60;

	/** Key for the Registry to save and load
	 *  the frame width for the graphpad frame
	 */
	protected static String FRAME_WIDTH = "FrameWidth";

	/** Key for the Registry to save and load
	 *  the frame height for the graphpad frame
	 */
	protected static String FRAME_HEIGHT = "FrameHeight";

	/** Key for the Registry to save and load
	 *  the frame state for the graphpad frame
	 */
	protected static String FRAME_STATE = "FrameState";

	/** Key for the Registry to save and load
	 *  the frame x position for the graphpad frame
	 */
	protected static String FRAME_X = "FrameX";

	/** Key for the Registry to save and load
	 *  the frame y position for the graphpad frame
	 */
	protected static String FRAME_Y = "FrameY";

	/** Boolean for the visible state of
	 *  the toolbars
	 */
	protected boolean toolBarsVisible = true;

	/** Global instance for some graph tool implementations.
	 * */
	protected static GPGraphTools graphTools = new GPGraphTools();


	/** Desktoppane for the internal frames
	 */
	protected JDesktopPane desktop = new JDesktopPane();

	/** Contains the mapping between GPDocument objects
	 *  and GPInternalFrames.
	 *
	 */
	protected Hashtable doc2InternalFrame = new Hashtable();

	/** A factory for the menu, tool and popup bars
	 *
	 */
	protected GPBarFactory barFactory = new GPBarFactory(this);

	/** The current Toolbar for this graphpad
	 */
	protected JPanel toolBarMainPanel = new JPanel(new BorderLayout());

	/** The current Toolbar for this graphpad
	 */
	protected JPanel toolBarInnerPanel;

	/** The current Menubar for this graphpad
	 */
	protected JMenuBar menubar;

	/** The current Statusbar for this Graphpad instance
	 */
	protected GPStatusBar statusbar;

	/** ActionMap contains all default Actions for
	 *  this application
	 *
	 */
	protected ActionMap defaultActionMap = new ActionMap();

	/** The main Panel with the status bar
	 *  and the desktop pane
	 */
	protected JPanel mainPanel = new JPanel(new BorderLayout());

	/** ActionMap contains the current ActionMap.
	 *
	 */
	protected ActionMap currentActionMap = defaultActionMap;

	protected GPMarqueeHandler marqueeHandler = new GPMarqueeHandler(this);

	protected GPConfiguration configuration = null;

	/** Creates a new default instance
	 */
	public GPGraphpad() {
		this(null);
	}

	/** Creates a new default instance
	 */
	public GPGraphpad(JGraphpad applet) {
		this(null, applet);
	}

	/** Creates a new instance with the
	 *  configuration from the config object.
	 *
	 *  @param config The configuration object.
	 */
	public GPGraphpad(GPConfiguration config, JGraphpad applet) {
		super(true);
		setApplet(applet);

		// procedure the configuration object
		configuration = config;

		// set the user actionmap if possible
		if (config != null && config.getActionMap() != null) {
			currentActionMap = config.getActionMap();
			currentActionMap.setParent(defaultActionMap);
		}

		// set the look and feel
		try {
			// Kunststoff makes scrolling extremely sluggish
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception exc) {
			try {
				UIManager.setLookAndFeel(
					UIManager.getCrossPlatformLookAndFeelClassName());
			} catch (Exception exc2) {
				System.err.println("Error loading L&F: " + exc);
			}
		}

		setBorder(BorderFactory.createEtchedBorder());
		setLayout(new BorderLayout());

		// create the statusbar
		createStatusBar();

		// use and fill the action map
		fillDefaultActionMap();

		// set my reference to the actions
		setMe4Actions();

		// build the menu and the toolbar
		menubar = barFactory.createMenubar();

		toolBarInnerPanel = barFactory.createToolBars(toolBarMainPanel);
		setToolBarsVisible(true);

		add(BorderLayout.NORTH, menubar);
		add(BorderLayout.CENTER, mainPanel);
		add(BorderLayout.SOUTH, statusbar);

		JFrame frame = createFrame();
		frame.setIconImage(applicationIcon.getImage());
		frame.setTitle(appTitle);
		frame.getContentPane().add(this);
		frame.addWindowListener(new AppCloser(frame));
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		// don't pack use the stored size
		//frame.pack();
		frame.show();

		update();
	}

	/** GPGraphpad uses the static initializer now.
	 *  Please do not use this method.
	 *  We will delete the method in one of the
	 *  next versions.
	 *
	 * @deprecated
	 */
	public static void init() {
	}
	/**
	 *  The initializes the common values
	 *
	 */
	static {
		try {
			String vers = System.getProperty("java.version");
			if (vers.compareTo("1.4") < 0) {
//TODO: need to open a dialog with instructions, if NOT HEADLESS
				System.out.println(
					"!!!WARNING: GraphEditor must be run with a "
						+ "1.4 or higher version VM!!!");
			}
			appTitle = "StreamIt GraphEditor";

			// Logo
			String iconName = Translator.getString("Logo");
			logoIcon = ImageLoader.getImageIcon(iconName);

			// Icon
			iconName = Translator.getString("Icon");
			applicationIcon = ImageLoader.getImageIcon(iconName);

		} catch (Throwable t) {
			System.out.println("uncaught exception: " + t);
			t.printStackTrace();
		}
	}

	/** Creates a frame for this Graphpad panel
	 *
	 */
	protected JFrame createFrame() {
		/*
		JFrame frame = new JFrameP() {
			public String getInstanceID() {
				return this.getClass().getName();
			}
			public String getNameSpace() {
				return "JGraphPad";
			}
		};
		*/
		GPFrame gpframe = new GPFrame();
		gpframe.setName("MainGraphpad");
		
		return gpframe;
	}

	/**
	 */
	public void fillDefaultActionMap() {
		for (int i = 0; i < defaultActions.length; i++) {
			defaultActionMap.put(
				defaultActions[i].getValue(Action.NAME),
				defaultActions[i]);
		}
	}

	protected void setMe4Actions() {
		Object[] keys;
		keys = defaultActionMap.allKeys();
		for (int i = 0; i < keys.length; i++) {
			Action a = defaultActionMap.get(keys[i]);
			if (a instanceof AbstractActionDefault) {
				AbstractActionDefault ad = (AbstractActionDefault) a;
				if (ad.getGraphpad() == null)
					ad.setGraphpad(this);
			}
		}

		if (currentActionMap == defaultActionMap)
			return;

		keys = currentActionMap.allKeys();
		for (int i = 0; i < keys.length; i++) {
			Action a = currentActionMap.get(keys[i]);
			if (a instanceof AbstractActionDefault) {
				AbstractActionDefault ad = (AbstractActionDefault) a;
				if (ad.getGraphpad() == null)
					ad.setGraphpad(this);
			}
		}
	}
	
/*
	public GPLogConsole getLogConsole() {
		return logger;
	}*/

	//modified by jcarlos
	/**
	 * Get the document with the corresponding IFile
	 */
	public GPDocument getDocumentWithIFile(IFile ifile)
	{
		GPDocument[] theDocuments = getAllDocuments();
		if (theDocuments != null)
		{

			for (int i = 0; i < theDocuments.length; i++)
			{
				// not sure if this should be .equals
				if (theDocuments[i].getIFile() == ifile)
				{
					return theDocuments[i];	
				}
			}
		}
		return null;
		
	}




	/** Log console for the System in and out
	 *  messages
	 */
	/*
	protected GPLogConsole logger = new GPLogConsole (
		appTitle, applicationIcon.getImage(),
		new Boolean(Translator.getString(
		"Error.makeLogDlgVisibleOnError")).booleanValue());
*/

	/** Returns the current Action Map
	 */
	public ActionMap getCurrentActionMap() {
		return currentActionMap;
	}

	/**
	 * To shutdown when run as an application.  This is a
	 * fairly lame implementation.   A more self-respecting
	 * implementation would at least check to see if a save
	 * was needed.
	 */
	protected final class AppCloser extends WindowAdapter {

		Frame frame;

		AppCloser(Frame f) {
			frame = f;
		}

		public void windowClosing(WindowEvent e) {

			// run the action			
			currentActionMap
				.get(Utilities.getClassNameWithoutPackage(FileExit.class))
				.actionPerformed(null);

			//System.exit(0);
		}
	}

	/**
	 * Find the hosting frame, for the file-chooser dialog.
	 */
	public Frame getFrame() {
		for (Container p = getParent(); p != null; p = p.getParent()) {
			if (p instanceof Frame) {
				return (Frame) p;
			}
		}
		return null;
	}

	public JMenuBar getMenubar() {
		return menubar;
	}

	/**
	 * Create a status bar
	 */
	protected GPStatusBar createStatusBar() {
		statusbar = new GPStatusBar();
		return statusbar;
	}

	public GPStatusBar getStatusBar() {
		return statusbar;
	}

	/** Show a dialog with the given error message.
	 * */
	public void error(String message) {
		JOptionPane.showMessageDialog(
			this,
			message,
			appTitle,
			JOptionPane.ERROR_MESSAGE);
	}

	// --- actions -----------------------------------

	/**
	 * Actions defined by the Graphpad class
	 */
	protected Action[] defaultActions =
		{
			new EditCell(this),
			new EditCopy(this),
			new EditCut(this),
			new EditDelete(this),
			new EditInsertIntoLibrary(this),
			new EditPaste(this),
			new EditProperties(this),
			new EditRedo(this),
			new EditUndo(this),
			new FileClose(this),
			new FileExit(this),
			new FileExportGraphviz(this),
			new FileExportImageMap(this),
			new FileExportJPG(this),
			new FileExportGIF(this),
			new FileExportPNG(this),
			new FileExportGXL(this),
			new FileLibraryClose(this),
			new FileLibraryNew(this),
			new FileLibraryOpen(this),
			new FileLibraryRename(this),
			new FileLibrarySaveAs(this),
			new FileNew(this),
			new FileNewView(this),
			new FileOpen(this),
			new FilePageFormat(this),
			new FilePrint(this),
			new FileSave(this),
			new FileSaveAs(this),
			new FormatAlignMiddle(this),
			new FormatAlignBottom(this),
			new FormatAlignCenter(this),
			new FormatAlignLeft(this),
			new FormatAlignRight(this),
			new FormatAlignTop(this),
			new FormatBorderColor(this),
			new FormatBorderColorList(this),
			new FormatBorderNo(this),
			new FormatFillColor(this),
			new FormatFillColorList(this),
			new FormatFillNo(this),
			new FormatShapeImage(this),
			new FormatShapeImageURL(this),
			new FormatShapeNoImage(this),
			new FormatLineBezier(this),
			new FormatLineColor(this),
			new FormatLineColorList(this),
			new FormatLineEndList(this),
			new FormatLineOrthogonal(this),
			new FormatLinePattern(this),
			new FormatLinePatternList(this),
			new FormatLineQuadratic(this),
			new FormatLineStartList(this),
			new FormatLineWidth(this),
			new FormatLineWidthList(this),
			new FormatLockNotBendable(this),
			new FormatLockNotConnectable(this),
			new FormatLockNotDisconnectable(this),
			new FormatLockPosition(this),
			new FormatLockSize(this),
			new FormatLockValue(this),
			new FormatReverse(this),
			new FormatRoutingNo(this),
			new FormatRoutingSimple(this),
			new FormatSizeAuto(this),
			new FormatSizeManual(this),
			new FormatStyleEndSize(this),
			new FormatStyleNoStartEnd(this),
			new FormatStyleStartSize(this),
			new FormatTextFont(this),
			new FormatTextFontColor(this),
			new FormatTextFontColorList(this),
			new FormatTextFontSize(this),
			new FormatTextFontSizeList(this),
			new FormatTextFontStyleItalic(this),
			new FormatTextPositionBottom(this),
			new FormatTextPositionCenter(this),
			new FormatTextPositionLeft(this),
			new FormatTextPositionMiddle(this),
			new FormatTextPositionRight(this),
			new FormatTextPositionTop(this),
			new FormatUnlockBendable(this),
			new FormatUnlockConnectable(this),
			new FormatUnlockConnections(this),
			new FormatUnlockDisconnectable(this),
			new FormatUnlockPosition(this),
			new FormatUnlockSize(this),
			new FormatUnlockValue(this),
			new GraphApplyLayoutAlgorithm(this),
			new GraphArrange(this),
			new GraphBackgroundColor(this),
			new GraphBackgroundImage(this),
			new GraphBackgroundNoImage(this),
			new GraphDragEnabled(this),
			new GraphDropEnabled(this),
			new GraphEditable(this),
			new GraphEnabled(this),
			new GraphLayout(this),
			new GraphOptionsBendable(this),
			new GraphOptionsCloneable(this),
			new GraphOptionsConnectable(this),
			new GraphOptionsDisconnectable(this),
			new GraphOptionsDisconnectOnMove(this),
			new GraphOptionsDoubleBuffered(this),
			new GraphOptionsMoveable(this),
			new GraphOptionsSizeable(this),
			new GraphTilt(this),
			new HelpAbout(this),
			new HelpSubmitABug(this),
			new HelpHomepage(this),
			new SelectAll(this),
			new SelectAllClear(this),
			new SelectEdges(this),
			new SelectEdgesClear(this),
			new SelectInverse(this),
			new SelectMinimalSpanTree(this),
			new SelectShortestPath(this),
			new SelectVertices(this),
			new SelectVerticesClear(this),
			new ShapeAlignBottom(this),
			new ShapeAlignCenter(this),
			new ShapeAlignLeft(this),
			new ShapeAlignMiddle(this),
			new ShapeAlignRight(this),
			new ShapeAlignTop(this),
			new ShapeBestPorts(this),
			new ShapeCloneAttributes(this),
			new ShapeCloneLabel(this),
			new ShapeCloneSize(this),
			new ShapeConnect(this),
			new ShapeDefaultPorts(this),
			new ShapeDisconnect(this),
			new ShapeGroup(this),
			new ShapeToBack(this),
			new ShapeToFront(this),
			new ShapeUngroup(this),
			new ToolBoxEdge(this),
			new ToolBoxFilter(this),
			new ToolBoxJoiner(this),
			new ToolBoxSplitJoin(this),
			new ToolBoxFeedbackLoop(this),
			new ToolBoxNode(this),
			new ToolBoxPipeline(this),
			new ToolBoxSelect(this),
			new ToolBoxSplitter(this),
			new ToolBoxZoomArea(this),
			new ToolsGridSize(this),
			new ToolsLookAndFeel(this),
			new ToolsMetric(this),
			new ToolsPortSize(this),
			new ToolsShowOverview(this),
			new ToolsShowStatusbar(this),
			new ToolsShowToolbar(this),
			new ToolsShowExplorer(this),
			new ToolsSnapSize(this),
			new ToolsStatusbar(this),
			new ToolsTooltips(this),
			new ViewActualSize(this),
			new ViewAntiAlias(this),
			new ViewFit(this),
			new ViewGrid(this),
			new ViewLayout(this),
			new ViewPorts(this),
			new ViewRuler(this),
			new ViewScaleCustom(this),
			new ViewScaleZoomIn(this),
			new ViewScaleZoomOut(this),
			new ViewExpand(this),
			new ViewCollapse(this),
			new WindowCascade(this),
			//new WindowLogConsole(this),
			new WindowMaximize(this),
			new WindowMinimize(this),
			new WindowWindows(this)};

	/**
	 * Returns the current graph.
	 * @return GPGraph
	 */
	public GPGraph getCurrentGraph() {
		GPDocument doc = getCurrentDocument();
		if (doc == null)
			return null;
		return doc.getGraph();
	}

	public GraphModelProvider getCurrentGraphModelProvider() {
		GPDocument doc = getCurrentDocument();
		if (doc == null)
			return null;
		return doc.getGraphModelProvider();
	}

	/**
	 * Returns the currently selected internal frame
	 * If no one is selected, then the first one will be select.
	 * 
	 * @return GPDocument
	 */
	public JInternalFrame getCurrentInternalFrame() {
		GPInternalFrame internalFrame =
			(GPInternalFrame) desktop.getSelectedFrame();
		if (internalFrame == null) {
			JInternalFrame[] frames = desktop.getAllFrames();
			if (frames.length > 0) {
				try {
					frames[0].setSelected(true);
					internalFrame = (GPInternalFrame) frames[0];
				} catch (PropertyVetoException e) {
					return null;
				}
			}
		}
		if (internalFrame == null)
			return null;
		return internalFrame;
	}

	/**
	 * Returns the currently selected document.
	 * If no one is selected, then the first one will be select.
	 * @return GPDocument
	 */
	public GPDocument getCurrentDocument() {
		GPInternalFrame internalFrame =
			(GPInternalFrame) desktop.getSelectedFrame();
		if (internalFrame == null) {
			JInternalFrame[] frames = desktop.getAllFrames();
			if (frames.length > 0) {
				try {
					frames[0].setSelected(true);
					internalFrame = (GPInternalFrame) frames[0];
				} catch (PropertyVetoException e) {
					return null;
				}
			}
		}
		if (internalFrame == null)
			return null;
		return internalFrame.getDocument();
	}

	/**
	 * Returns all of the documents.
	 * @return GPGraphTools or <code>null</code> if no documents
	 */
	public GPDocument[] getAllDocuments() {
		JInternalFrame[] frames = desktop.getAllFrames();

		if (frames != null && frames.length > 0) {
			ArrayList docs = new ArrayList();
			for (int i = 0; i < frames.length; i++) {
				// make sure to only pick up GPInternalFrame instances
				if (frames[i] instanceof GPInternalFrame) {
					docs.add(((GPInternalFrame) frames[i]).getDocument());
				}
			}
			return (GPDocument[]) docs.toArray(new GPDocument[docs.size()]);
		} else
			return null;
	}

	/**
	 * Returns the graphTools.
	 * @return GPGraphTools
	 */
	public static GPGraphTools getGraphTools() {
		return graphTools;
	}

	/**
	 * Returns the undoAction.
	 * @return UndoAction
	 */
	public AbstractActionDefault getEditUndoAction() {
		return (AbstractActionDefault) currentActionMap.get(
			Utilities.getClassNameWithoutPackage(EditUndo.class));
	}

	/**
	 * Returns the redoAction.
	 *
	 * @return RedoAction
	 */
	public AbstractActionDefault getEditRedoAction() {
		return (AbstractActionDefault) currentActionMap.get(
			Utilities.getClassNameWithoutPackage(EditRedo.class));
	}


	public boolean isToolBarsVisible() {
		return this.toolBarsVisible;
	}

	public void setToolBarsVisible(boolean state) {
		this.toolBarsVisible = state;

		if (state == true) {
			mainPanel.remove(desktop);
			toolBarInnerPanel.add(BorderLayout.CENTER, desktop);
			mainPanel.add(BorderLayout.CENTER, toolBarMainPanel);
		} else {
			mainPanel.remove(toolBarMainPanel);
			toolBarInnerPanel.remove(desktop);
			mainPanel.add(BorderLayout.CENTER, desktop);
		}
		desktop.repaint();
	}

	/** Adds a new Internal Frame to the Graphpad
	 */
	public void addGPInternalFrame(GPInternalFrame f) {
		desktop.add(f);
		try {
			f.setSelected(true);
		} catch (Exception ex) {
		}
		doc2InternalFrame.put(f.getDocument(), f);
	}

	/** removes the specified Internal Frame from the Graphpad
	 */
	public void removeGPInternalFrame(GPInternalFrame f) {
		if (f == null)
			return;
		f.setVisible(false);
		desktop.remove(f);
		doc2InternalFrame.remove(f.getDocument());
		JInternalFrame[] frames = desktop.getAllFrames();
		if (frames.length > 0) {
			try {
				frames[0].setSelected(true);
			} catch (PropertyVetoException e) {
			}
		}
	}

	/** Adds a new Document based on the GraphModelProvider.
	 *
	 */
	public void exit() {

		if (!isApplet()) {
			System.exit(0);
		} else {
			getApplet().exit(this);
		}
	}

	/** Adds a new Document based on the GraphModelProvider.
	 *
	 */
	public void setApplet(JGraphpad applet) {
		this.applet = applet;
	}

	/** Adds a new Document based on the GraphModelProvider.
	 *
	 */
	public JGraphpad getApplet() {
		return applet;
	}

	/** Adds a new Document based on the GraphModelProvider.
	 *
	 */
	public boolean isApplet() {
		return (applet != null);
	}

	/** Adds a new Document based on the GraphModelProvider.
	 *
	 */
	public void addDocument(GraphModelProvider graphModelProvider) {
		addDocument(null, graphModelProvider, null, null, null);
	}

	/**
	 * You can add a document by giving the filename.
	 * Before you can add a document with the specified filename
	 * you must add the corresponding GraphModelProvider and the
	 * corresponding GraphModelFileFormat at the GraphModelProviderRegistry.
	 *
	 * @see org.jgraph.pad.GraphModelProviderRegistry
	 * @see org.jgraph.pad.GraphModelProvider
		 * @see org.jgraph.pad.GraphModelFileFormat
		 *
	 */
	public void addDocument(URL file) {
		GraphModelProvider graphModelProvider = null;
		GraphModelFileFormat fileFormat = null;
		try {
			graphModelProvider =
				GraphModelProviderRegistry.getGraphModelProvider(
					file.toString());
			fileFormat =
				GraphModelProviderRegistry.getGraphModelFileFormat(
					file.toString());

			// try to use the defaults, 
			// if there is no provider available
			if (graphModelProvider == null)
				graphModelProvider = new DefaultGraphModelProvider();
		
			if (fileFormat == null)
				//modified by jcarlos fileFormat = new DefaultGraphModelFileFormatXML();
				fileFormat = new DefaultGraphModelFileFormatStreamIt();

			/* Don't do this call directly!
			 * We have a multi file format support 
			 * and you kick it out with this code!
			 * 
			 *
			GraphModelProvider graphModelProvider =
				new DefaultGraphModelProvider();
			GraphModelFileFormat fileFormat =
				new DefaultGraphModelFileFormatXML();
			*/

			GraphModel model = graphModelProvider.createCleanGraphModel();
			GPGraph gpGraph = graphModelProvider.createCleanGraph(model);
			fileFormat.read(file, null, gpGraph);
			addDocument(file, graphModelProvider, gpGraph, model, null);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(
				getFrame(),
				e
					+ "\nFile="
					+ file
					+ "\nFileFormat="
					+ fileFormat
					+ "\nGraphModelProvider="
					+ graphModelProvider,
				Translator.getString("Error"),
				JOptionPane.ERROR_MESSAGE);
		}
	}


	/** Opens a new document frame based on the parameters
	 *
	 * */
	public GPDocument addDocument(
		URL file,
		GraphModelProvider graphModelProvider,
		GPGraph gpGraph,
		GraphModel model,
		GraphUndoManager undo) {

		if (graphModelProvider == null)
			return new GPDocument(null, null, null, null, null, null);

		if (model == null)
			model = graphModelProvider.createCleanGraphModel();

		if (gpGraph == null) {
			gpGraph = graphModelProvider.createCleanGraph(model);
		}

		gpGraph.setSelectNewCells(true);
		gpGraph.setInvokesStopCellEditing(true);
		gpGraph.setMarqueeHandler(getMarqueeHandler());

		GPDocument doc =
			new GPDocument(
				this,
				file,
				graphModelProvider,
				gpGraph,
				model,
				null);

		GPInternalFrame iframe = new GPInternalFrame(doc);
		addGPInternalFrame(iframe);
		iframe.show();
		/*
		try {
			//iframe.setMaximum(true);
		} catch (PropertyVetoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/

		iframe.grabFocus();
		return doc;
	}

	/** Opens a new document frame based on the parameters
	 * 	added by jcarlos
	 * */
	public GPDocument addDocument(
		URL file,
		GraphModelProvider graphModelProvider,
		GPGraph gpGraph,
		GraphModel model,
		GraphStructure graphStruct,
		GraphUndoManager undo) {

		if (graphModelProvider == null)
			return new GPDocument(null, null, null, null, null, null);

		if (model == null)
			model = graphModelProvider.createCleanGraphModel();

		if (gpGraph == null) {
			gpGraph = graphModelProvider.createCleanGraph(model);
		}

		gpGraph.setSelectNewCells(true);
		gpGraph.setInvokesStopCellEditing(true);
		gpGraph.setMarqueeHandler(getMarqueeHandler());

		GPDocument doc =
			new GPDocument(
				this,
				file,
				graphModelProvider,
				gpGraph,
				model,
				graphStruct,
				null);

		GPInternalFrame iframe = new GPInternalFrame(doc);
		addGPInternalFrame(iframe);
		iframe.show();
		/*
		try {
			//iframe.setMaximum(true);
		} catch (PropertyVetoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/

		iframe.grabFocus();
		return doc;
	}




	public void removeDocument(GPDocument doc) {
		GPInternalFrame iFrame = (GPInternalFrame) doc2InternalFrame.get(doc);
		removeGPInternalFrame(iFrame);
	}

	public void update() {
		GPDocument currentDoc = getCurrentDocument();

		Object[] keys = currentActionMap.keys();
		for (int i = 0; i < keys.length; i++) {
			Action a = currentActionMap.get(keys[i]);
			if (a instanceof AbstractActionDefault) {
				((AbstractActionDefault) a).update();
			} else {
				if (currentDoc == null) {
					a.setEnabled(false);
				} else {
					a.setEnabled(true);
				}
			}
		}

		if (currentActionMap == defaultActionMap)
			return;

		keys = defaultActionMap.keys();
		for (int i = 0; i < keys.length; i++) {
			Action a = defaultActionMap.get(keys[i]);
			if (a instanceof AbstractActionDefault) {
				((AbstractActionDefault) a).update();
			}
		}
	}

	/**
	 * Returns the barFactory.
	 * @return GPBarFactory
	 */
	public GPBarFactory getBarFactory() {
		return barFactory;
	}

	/**
	 * Sets the barFactory.
	 * @param barFactory The barFactory to set
	 */
	public void setBarFactory(GPBarFactory barFactory) {
		this.barFactory = barFactory;
	}

	public JInternalFrame[] getAllFrames() {
		return desktop.getAllFrames();
	}

	public void setParentActionMap(ActionMap map) {
		defaultActionMap.setParent(map);
	}
	/**
	 * Returns the applicationIcon.
	 * @return ImageIcon
	 */
	public static ImageIcon getApplicationIcon() {
		return applicationIcon;
	}

	/**
	 * Sets the applicationIcon.
	 * @param applicationIcon The applicationIcon to set
	 */
	public static void setApplicationIcon(ImageIcon applicationIcon) {
		GPGraphpad.applicationIcon = applicationIcon;
	}

	/**
	 * Returns the marqueeHandler.
	 * @return GPMarqueeHandler
	 */
	public GPMarqueeHandler getMarqueeHandler() {
		return marqueeHandler;
	}

	public void addDesktopContainerListener(ContainerListener listener) {
		desktop.addContainerListener(listener);
	}

	public void removeDesktopContainerListener(ContainerListener listener) {
		desktop.removeContainerListener(listener);
	}
}
