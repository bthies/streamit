// This file is part of the Echidna project
// (C) 2002 Forschungszentrum Informatik (FZI) Karlsruhe
// Please visit our website at http://echidna.sf.net
package streamit.eclipse.grapheditor.editor.layout;

import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JOptionPane;

import org.jgraph.JGraph;
import org.jgraph.graph.CellView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.VertexView;

import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 * Arranges the nodes with the Sugiyama Layout Algorithm.<br>
 *
 * <a href="http://plg.uwaterloo.ca/~itbowman/CS746G/Notes/Sugiyama1981_MVU/">
 *  Link to the algorithm</a>
 *
 *<br>
 *<br>
 * @author Sven Luzar<br>
 * @version 1.0 init
 */
public class SugiyamaLayoutAlgorithm implements LayoutAlgorithm {

	/** Field for debug output
	 */
	protected final boolean verbose = false;

	/** Const to add Attributes at the Nodes
	 *
	 */
	public static final String SUGIYAMA_VISITED = "SugiyamaVisited" /*#Frozen*/;

	/** Const to add the Cell Wrapper to the Nodes
	 */
	public static final String SUGIYAMA_CELL_WRAPPER =
		"SugiyamaCellWrapper" /*#Frozen*/;

	/** represents the size of the grid in horizontal grid elements
	 *
	 */
	protected int gridAreaSize = Integer.MIN_VALUE;

	/** Progressbar is shown while the algorithm is running
	 */
	protected ProgressDialog dlgProgress =
		new ProgressDialog(
			(Frame) null,
			Translator.getString("Progress") + ":",
			false);
	/* #Finished */

	/** A vector with Integer Objects. The Vector contains the
	 *  history of movements per loop
	 *  It was needed for the progress dialog
	 */
	Vector movements = null;
	/** Represents the movements in the current loop.
	 *  It was needed for the progress dialog
	 */
	int movementsCurrentLoop = -1;
	/** Represents the maximum of movements in the current loop.
	 *  It was needed for the progress dialog
	 */
	int movementsMax = Integer.MIN_VALUE;
	/** Represents the current loop number
	 *  It was needed for the progress dialog
	 */
	int iteration = 0;

	/**
	 * Implementation.
	 *
	 * First of all the Algorithm searches the roots from the
	 * Graph. Starting from this roots the Algorithm creates
	 * levels and stores them in the member <code>levels</code>.
	 * The Member levels contains Vector Objects and the Vector per level
	 * contains Cell Wrapper Objects. After that the Algorithm
	 * tries to solve the edge crosses from level to level and
	 * goes top down and bottom up. After minimization of the
	 * edge crosses the algorithm moves each node to its
	 * bary center. Last but not Least the method draws the Graph.
	 *
	 * @see LayoutAlgorithm
	 *
	 */
	public void perform(
		JGraph jgraph,
		boolean applyToAll,
		Properties configuration) {

		Object[] selectedCells =
			(applyToAll ? jgraph.getRoots() : jgraph.getSelectionCells());
		CellView[] selectedCellViews =
			jgraph.getGraphLayoutCache().getMapping(selectedCells);

		Point spacing = new Point();
		/*  The Algorithm distributes the nodes on a grid.
		 *  For this grid you can configure the horizontal spacing.
		 *  This field specifies the configured value
		 *
		 */
		spacing.x =
			Integer.parseInt(
				configuration.getProperty(
					SugiyamaLayoutController.KEY_HORIZONTAL_SPACING));

		/*  The Algorithm distributes the nodes on a grid.
		 *  For this grid you can configure the vertical spacing.
		 *  This field specifies the configured value
		 *
		 */

		spacing.y =
			Integer.parseInt(
				configuration.getProperty(
					SugiyamaLayoutController.KEY_VERTICAL_SPACING));

		// set the progress dialog visible
		dlgProgress.setVisible(true);

		// search all roots
		Vector roots = searchRoots(jgraph, selectedCellViews);

		// return if no root found
		if (roots.size() == 0)
			return;

		// create levels
		Vector levels = fillLevels(jgraph, selectedCellViews, roots);

		// solves the edge crosses
		solveEdgeCrosses(jgraph, levels);

		// move all nodes into the barycenter
		moveToBarycenter(jgraph, selectedCellViews, levels);

		Point min = findMinimumAndSpacing(selectedCellViews, spacing);

		// draw the graph in the window
		drawGraph(jgraph, levels, min, spacing);

		// clean temp values from the nodes / cells
		// the clean up was made in drawGraph
		//cleanUp(selectedCellViews);

		// sets the progress dialog unvisible
		dlgProgress.setVisible(false);
	}

	/** Debugdisplay for the edge crosses indicators on the System out
	 */
	protected void displayEdgeCrossesValues(Vector levels) {
		System.out.println("----------------Edge Crosses Indicator Values"
		/*#Frozen*/
		);

		for (int i = 0; i < levels.size() - 1; i++) {
			// Get the current level
			Vector currentLevel = (Vector) levels.get(i);
			System.out.print("Level (" + i + "):" /*#Frozen*/
			);
			for (int j = 0; j < currentLevel.size(); j++) {
				CellWrapper sourceWrapper = (CellWrapper) currentLevel.get(j);

				System
					.out
					.print(
						NumberFormat.getNumberInstance().format(
							sourceWrapper.getEdgeCrossesIndicator())
						+ " - " /*#Frozen*/
				);
			}
			System.out.println();
		}
	}

	/** Debugdisplay for the grid positions on the System out
	 */
	protected void displayGridPositions(Vector levels) {

		System.out.println("----------------GridPositions" /*#Frozen*/
		);

		for (int i = 0; i < levels.size() - 1; i++) {
			// Get the current level
			Vector currentLevel = (Vector) levels.get(i);
			System.out.print("Level (" + i + "):" /*#Frozen*/
			);
			for (int j = 0; j < currentLevel.size(); j++) {
				CellWrapper sourceWrapper = (CellWrapper) currentLevel.get(j);
				System
					.out
					.print(
						NumberFormat.getNumberInstance().format(
							sourceWrapper.getGridPosition())
						+ " - " /*#Frozen*/
				);
			}
			System.out.println();
		}
	}

	/** Debugdisplay for the priorities on the System out
	 */
	protected void displayPriorities(Vector levels) {

		System.out.println("----------------down Priorities" /*#Frozen*/
		);

		for (int i = 0; i < levels.size() - 1; i++) {
			// Get the current level
			Vector currentLevel = (Vector) levels.get(i);
			System.out.print("Level (" + i + "):" /*#Frozen*/
			);
			for (int j = 0; j < currentLevel.size(); j++) {
				CellWrapper sourceWrapper = (CellWrapper) currentLevel.get(j);
				System.out.print(sourceWrapper.getPriority() +
				/*" (" +
				                   sourceWrapper.nearestDownNeighborLevel + ") " +*/
				" - " /*#Frozen*/
				);
			}
			System.out.println();
		}
	}

	/** Searches all Roots for the current Graph
	 *  First the method marks any Node as not visited.
	 *  Than calls searchRoots(MyGraphCell) for each
	 *  not visited Cell.
	 *  The Roots are stored in the Vector named roots
	 *
	 * 	@return returns a Vector with the roots
	 *  @see #searchRoots(JGraph, CellView[])
	 */
	protected Vector searchRoots(JGraph jgraph, CellView[] selectedCellViews) {

		// get all cells and relations
		Vector vertexViews = new Vector(selectedCellViews.length);
		Vector roots = new Vector();

		// first: mark all as not visited
		// O(allCells&Edges)
		for (int i = 0; i < selectedCellViews.length; i++) {
			if (selectedCellViews[i] instanceof VertexView) {
				VertexView vertexView = (VertexView) selectedCellViews[i];
				vertexView.getAttributes().remove(SUGIYAMA_VISITED);
				vertexViews.add(selectedCellViews[i]);
			}
		}

		// O(graphCells)
		for (int i = 0; i < vertexViews.size(); i++) {
			VertexView vertexView = (VertexView) vertexViews.get(i);
			if (vertexView.getAttributes().get(SUGIYAMA_VISITED) == null) {
				searchRoots(jgraph, vertexView, roots);
			}
		}

		// Error Msg if the graph has no roots
		if (roots.size() == 0) {
			System.out.println("****** THE GRAPH HAS NO ROOTS *****");
			JOptionPane
				.showMessageDialog(
					null,
					Translator.getString("TheGraphIsNotADAG"
			/*#Finished:Original="The Graph is not a DAG. Can't use Sugiyama Algorithm!"*/
			), null, JOptionPane.ERROR_MESSAGE);
		}
		return roots;
	}

	/** Searches Roots for the current Cell.
	 *
	 *  Therefore he looks at all Ports from the Cell.
	 *  At the Ports he looks for Edges.
	 *  At the Edges he looks for the Target.
	 *  If the Ports of the current Cell contains the target ReViewNodePort
	 *  he follows the edge to the source and looks at the
	 *  Cell for this source.
	 *
	 *  @param graphCell The current cell
	 */
	protected void searchRoots(
		JGraph jgraph,
		VertexView vertexViewToInspect,
		Vector roots) {
		// the node already visited
		if (vertexViewToInspect.getAttributes().get(SUGIYAMA_VISITED)
			!= null) {
			return;
		}

		// mark as visited for cycle tests
		vertexViewToInspect.getAttributes().put(
			SUGIYAMA_VISITED,
			new Boolean(true));

		GraphModel model = jgraph.getModel();

		// get all Ports and search the relations at the ports
		//List vertexPortViewList = new ArrayList() ;

		Object vertex = vertexViewToInspect.getCell();

		int portCount = model.getChildCount(vertex);
		for (int j = 0; j < portCount; j++) {
			Object port = model.getChild(vertex, j);

			// Test all relations for where
			// the current node is a target node
			// for roots

			boolean isRoot = true;
			Iterator itrEdges = model.edges(port);
			while (itrEdges.hasNext()) {
				Object edge = itrEdges.next();

				// if the current node is a target node
				// get the source node and test
				// the source node for roots

				if (model.getTarget(edge) == port) {
					Object sourcePort = model.getSource(edge);

					Object sourceVertex = model.getParent(sourcePort);

					CellView sourceVertexView =
						jgraph.getGraphLayoutCache().getMapping(
							sourceVertex,
							false);
					if (sourceVertexView instanceof VertexView) {
						searchRoots(
							jgraph,
							(VertexView) sourceVertexView,
							roots);
						isRoot = false;
					}
				}
			}
			// The current node is never a Target Node
			// -> The current node is a root node
			if (isRoot) {
				roots.add(vertexViewToInspect);
			}
		}
	}

	/** Method fills the levels and stores them in the member levels.
	
	 *  Each level was represended by a Vector with Cell Wrapper objects.
	 *  These Vectors are the elements in the <code>levels</code> Vector.
	 *
	 */
	protected Vector fillLevels(
		JGraph jgraph,
		CellView[] selectedCellViews,
		Vector rootVertexViews) {
		Vector levels = new Vector();

		// mark as not visited
		// O(allCells)
		for (int i = 0; i < selectedCellViews.length; i++) {
			CellView cellView = selectedCellViews[i];

			// more stabile
			if (cellView == null)
				continue;

			cellView.getAttributes().remove(SUGIYAMA_VISITED);
		}

		Enumeration enumRoots = rootVertexViews.elements();
		while (enumRoots.hasMoreElements()) {
			VertexView vertexView = (VertexView) enumRoots.nextElement();
			fillLevels(jgraph, levels, 0, vertexView);
		}

		return levels;

	}

	/** Fills the Vector for the specified level with a wrapper
	 *  for the MyGraphCell. After that the method called for
	 *  each neighbor graph cell.
	 *
	 *  @param level        The level for the graphCell
	 *  @param graphCell    The Graph Cell
	 */
	protected void fillLevels(
		JGraph jgraph,
		Vector levels,
		int level,
		VertexView vertexView) {
		// precondition control
		if (vertexView == null)
			return;

		// be sure that a Vector container exists for the current level
		if (levels.size() == level)
			levels.insertElementAt(new Vector(), level);

		// if the cell already visited return
		if (vertexView.getAttributes().get(SUGIYAMA_VISITED) != null) {
			return;
		}

		// mark as visited for cycle tests
		vertexView.getAttributes().put(SUGIYAMA_VISITED, new Boolean(true));

		// put the current node into the current level
		// get the Level Vector
		Vector vecForTheCurrentLevel = (Vector) levels.get(level);

		// Create a wrapper for the node
		int numberForTheEntry = vecForTheCurrentLevel.size();

		CellWrapper wrapper =
			new CellWrapper(level, numberForTheEntry, vertexView);

		// put the Wrapper in the LevelVector
		vecForTheCurrentLevel.add(wrapper);

		// concat the wrapper to the cell for an easy access
		vertexView.getAttributes().put(SUGIYAMA_CELL_WRAPPER, wrapper);

		// if the Cell has no Ports we can return, there are no relations
		Object vertex = vertexView.getCell();
		GraphModel model = jgraph.getModel();
		int portCount = model.getChildCount(vertex);

		// iterate any NodePort
		for (int i = 0; i < portCount; i++) {

			Object port = model.getChild(vertex, i);

			// iterate any Edge in the port
			Iterator itrEdges = model.edges(port);

			while (itrEdges.hasNext()) {
				Object edge = itrEdges.next();

				// if the Edge is a forward edge we should follow this edge
				if (port == model.getSource(edge)) {
					Object targetPort = model.getTarget(edge);
					Object targetVertex = model.getParent(targetPort);
					VertexView targetVertexView =
						(VertexView) jgraph.getGraphLayoutCache().getMapping(
							targetVertex,
							false);
					fillLevels(jgraph, levels, (level + 1), targetVertexView);
				}
			}
		}

		if (vecForTheCurrentLevel.size() > gridAreaSize) {
			gridAreaSize = vecForTheCurrentLevel.size();
		}

	}

	/** calculates the minimum for the paint area.
	 *
	 */
	protected Point findMinimumAndSpacing(
		CellView[] graphCellViews,
		Point spacing) {
		try {

			// variables
			/* represents the minimum x value for the paint area
			 */
			int min_x = 1000000;

			/* represents the minimum y value for the paint area
			 */
			int min_y = 1000000;

			// find the maximum & minimum coordinates

			for (int i = 0; i < graphCellViews.length; i++) {

				// the cellView and their bounds
				CellView cellView = graphCellViews[i];

				if (cellView == null)
					continue;
					
				Rectangle cellViewBounds = cellView.getBounds();

				// checking min area
				try {
					if (cellViewBounds.x < min_x)
						min_x = cellViewBounds.x;
					if (cellViewBounds.y < min_y)
						min_y = cellViewBounds.y;
					/*
					if (cellViewBounds.width > spacing.x)
						spacing.x = cellViewBounds.width;
					if (cellViewBounds.height > spacing.y)
						spacing.y = cellViewBounds.height;
						*/

				} catch (Exception e) {
					System.err.println("---------> ERROR in calculateValues."
					/*#Frozen*/
					);
					e.printStackTrace();
				}
			}
			// if the cell sice is bigger than the userspacing
			// dublicate the spacingfactor
			return new Point(min_x, min_y);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/** Updates the progress based on the movements count
	 *
	 */
	protected void updateProgress4Movements() {
		// adds the current loop count
		movements.add(new Integer(movementsCurrentLoop));
		iteration++;

		// if the current loop count is higher than the max movements count
		// memorize the new max
		if (movementsCurrentLoop > movementsMax) {
			movementsMax = movementsCurrentLoop;
		}

		// Calculate the new progress
		if (movements.size() > 1) {
			dlgProgress.setValue(movements.size() - 1);
		}
	}

	protected void solveEdgeCrosses(JGraph jgraph, Vector levels) {
		dlgProgress.setMessage(Translator.getString("SolvingCrossOverPoints"
		/*#Finished:Original="solving cross over points ..."*/
		));

		movements = new Vector(100);
		movementsCurrentLoop = -1;
		movementsMax = Integer.MIN_VALUE;
		iteration = 0;

		while (movementsCurrentLoop != 0) {

			// reset the movements per loop count
			movementsCurrentLoop = 0;

			if (verbose) {
				System.out.println("---------------------------- vor Sort"
				/*#Frozen*/
				);
				displayEdgeCrossesValues(levels);
			}

			// top down
			for (int i = 0; i < levels.size() - 1; i++) {
				movementsCurrentLoop
					+= solveEdgeCrosses(jgraph, true, levels, i);
			}

			// bottom up
			for (int i = levels.size() - 1; i >= 1; i--) {
				movementsCurrentLoop
					+= solveEdgeCrosses(jgraph, false, levels, i);
			}

			if (verbose) {
				System.out.println("---------------------------- nach Sort"
				/*#Frozen*/
				);
				displayEdgeCrossesValues(levels);
			}

			updateProgress4Movements();
		}
		dlgProgress.setToMaximum();
	}

	/**
	 *  @return movements
	 */
	protected int solveEdgeCrosses(
		JGraph jgraph,
		boolean down,
		Vector levels,
		int levelIndex) {
		// Get the current level
		Vector currentLevel = (Vector) levels.get(levelIndex);
		int movements = 0;

		// restore the old sort
		Object[] levelSortBefore = currentLevel.toArray();

		// new sort
		Collections.sort(currentLevel);

		// test for movements
		for (int j = 0; j < levelSortBefore.length; j++) {
			if (((CellWrapper) levelSortBefore[j]).getEdgeCrossesIndicator()
				!= ((CellWrapper) currentLevel.get(j))
					.getEdgeCrossesIndicator()) {
				movements++;

			}
		}

		GraphModel model = jgraph.getModel();

		// Collecations Sort sorts the highest value to the first value
		for (int j = currentLevel.size() - 1; j >= 0; j--) {
			CellWrapper sourceWrapper = (CellWrapper) currentLevel.get(j);

			VertexView sourceView = sourceWrapper.getVertexView();

			Object sourceVertex = sourceView.getCell();
			int sourcePortCount = model.getChildCount(sourceVertex);

			for (int k = 0; k < sourcePortCount; k++) {
				Object sourcePort = model.getChild(sourceVertex, k);

				Iterator sourceEdges = model.edges(sourcePort);
				while (sourceEdges.hasNext()) {
					Object edge = sourceEdges.next();

					// if it is a forward edge follow it
					Object targetPort = null;
					if (down && sourcePort == model.getSource(edge)) {
						targetPort = model.getTarget(edge);
					}
					if (!down && sourcePort == model.getTarget(edge)) {
						targetPort = model.getSource(edge);
					}
					if (targetPort == null)
						continue;

					Object targetCell = model.getParent(targetPort);
					VertexView targetVertexView =
						(VertexView) jgraph.getGraphLayoutCache().getMapping(
							targetCell,
							false);

					if (targetVertexView == null)
						continue;

					CellWrapper targetWrapper =
						(CellWrapper) targetVertexView.getAttributes().get(
							SUGIYAMA_CELL_WRAPPER);

					// do it only if the edge is a forward edge to a deeper level
					if (down
						&& targetWrapper != null
						&& targetWrapper.getLevel() > levelIndex) {
						targetWrapper.addToEdgeCrossesIndicator(
							sourceWrapper.getEdgeCrossesIndicator());
					}
					if (!down
						&& targetWrapper != null
						&& targetWrapper.getLevel() < levelIndex) {
						targetWrapper.addToEdgeCrossesIndicator(
							sourceWrapper.getEdgeCrossesIndicator());
					}
				}
			}
		}

		return movements;
	}

	protected void moveToBarycenter(
		JGraph jgraph,
		CellView[] allSelectedViews,
		Vector levels) {

		//================================================================
		// iterate any ReViewNodePort
		GraphModel model = jgraph.getModel();
		for (int i = 0; i < allSelectedViews.length; i++) {
			if (!(allSelectedViews[i] instanceof VertexView))
				continue;

			VertexView vertexView = (VertexView) allSelectedViews[i];

			CellWrapper currentwrapper =
				(CellWrapper) vertexView.getAttributes().get(
					SUGIYAMA_CELL_WRAPPER);

			Object vertex = vertexView.getCell();
			int portCount = model.getChildCount(vertex);

			for (int k = 0; k < portCount; k++) {
				Object port = model.getChild(vertex, k);

				// iterate any Edge in the port

				Iterator edges = model.edges(port);
				while (edges.hasNext()) {
					Object edge = edges.next();

					Object neighborPort = null;
					// if the Edge is a forward edge we should follow this edge
					if (port == model.getSource(edge)) {
						neighborPort = model.getTarget(edge);
					} else {
						if (port == model.getTarget(edge)) {
							neighborPort = model.getSource(edge);
						} else {
							continue;
						}
					}

					Object neighborVertex = model.getParent(neighborPort);

					VertexView neighborVertexView =
						(VertexView) jgraph.getGraphLayoutCache().getMapping(
							neighborVertex,
							false);

					if (neighborVertexView == null
						|| neighborVertexView == vertexView)
						continue;

					CellWrapper neighborWrapper =
						(CellWrapper) neighborVertexView.getAttributes().get(
							SUGIYAMA_CELL_WRAPPER);

					if (currentwrapper == null
						|| neighborWrapper == null
						|| currentwrapper.level == neighborWrapper.level)
						continue;

					currentwrapper.priority++;

				}
			}
		}

		//================================================================
		for (int j = 0; j < levels.size(); j++) {
			Vector level = (Vector) levels.get(j);
			for (int i = 0; i < level.size(); i++) {
				// calculate the initial Grid Positions 1, 2, 3, .... per Level
				CellWrapper wrapper = (CellWrapper) level.get(i);
				wrapper.setGridPosition(i);
			}
		}

		if (verbose) {
			System.out.println("----------------Grid Pos before top down"
			/*#Frozen*/
			);
			displayPriorities(levels);
			displayGridPositions(levels);
			System.out.println("======================================="
			/*#Frozen*/
			);
		}

		movements = new Vector(100);
		movementsCurrentLoop = -1;
		movementsMax = Integer.MIN_VALUE;
		iteration = 0;

		//int movements = 1;

		while (movementsCurrentLoop != 0) {

			// reset movements
			movementsCurrentLoop = 0;

			// top down
			for (int i = 1; i < levels.size(); i++) {
				movementsCurrentLoop += moveToBarycenter(jgraph, levels, i);
			}

			if (verbose) {
				System.out.println("----------------Grid Pos after top down"
				/*#Frozen*/
				);
				displayGridPositions(levels);
				System.out.println("======================================="
				/*#Frozen*/
				);
			}

			// bottom up
			for (int i = levels.size() - 1; i >= 0; i--) {
				movementsCurrentLoop += moveToBarycenter(jgraph, levels, i);
			}

			if (verbose) {
				System.out.println("----------------Grid Pos after bottom up"
				/*#Frozen*/
				);
				displayGridPositions(levels);
				//displayDownPriorities();
				System.out.println("======================================="
				/*#Frozen*/
				);
			}

			this.updateProgress4Movements();
		}

	}

	protected int moveToBarycenter(
		JGraph jgraph,
		Vector levels,
		int levelIndex) {

		// Counter for the movements
		int movements = 0;

		// Get the current level
		Vector currentLevel = (Vector) levels.get(levelIndex);
		GraphModel model = jgraph.getModel();

		for (int currentIndexInTheLevel = 0;
			currentIndexInTheLevel < currentLevel.size();
			currentIndexInTheLevel++) {

			CellWrapper sourceWrapper =
				(CellWrapper) currentLevel.get(currentIndexInTheLevel);

			float gridPositionsSum = 0;
			float countNodes = 0;

			VertexView vertexView = sourceWrapper.getVertexView();
			Object vertex = vertexView.getCell();
			int portCount = model.getChildCount(vertex);

			for (int i = 0; i < portCount; i++) {
				Object port = model.getChild(vertex, i);

				Iterator edges = model.edges(port);
				while (edges.hasNext()) {
					Object edge = edges.next();

					// if it is a forward edge follow it
					Object neighborPort = null;
					if (port == model.getSource(edge)) {
						neighborPort = model.getTarget(edge);
					} else {
						if (port == model.getTarget(edge)) {
							neighborPort = model.getSource(edge);
						} else {
							continue;
						}
					}

					Object neighborVertex = model.getParent(neighborPort);

					VertexView neighborVertexView =
						(VertexView) jgraph.getGraphLayoutCache().getMapping(
							neighborVertex,
							false);
					
					if (neighborVertexView == null)
						continue;
							
					CellWrapper targetWrapper =
						(CellWrapper) neighborVertexView.getAttributes().get(
							SUGIYAMA_CELL_WRAPPER);

					if (targetWrapper == sourceWrapper)
						continue;
					if (targetWrapper == null
						|| targetWrapper.getLevel() == levelIndex)
						continue;

					gridPositionsSum += targetWrapper.getGridPosition();
					countNodes++;
				}
			}

			//----------------------------------------------------------
			// move node to new x coord
			//----------------------------------------------------------

			if (countNodes > 0) {
				float tmp = (gridPositionsSum / countNodes);
				int newGridPosition = Math.round(tmp);
				boolean toRight =
					(newGridPosition > sourceWrapper.getGridPosition());

				boolean moved = true;

				while (newGridPosition != sourceWrapper.getGridPosition()
					&& moved) {
					int tmpGridPos = sourceWrapper.getGridPosition();

					moved =
						move(
							toRight,
							currentLevel,
							currentIndexInTheLevel,
							sourceWrapper.getPriority());

					if (moved)
						movements++;

					if (verbose) {

						System
							.out
							.print(
								"try move at Level "
								+ levelIndex
								+ " with index "
								+ currentIndexInTheLevel
								+ " to "
								+ (toRight ? "Right" : "Left")
								+ " CurrentGridPos: "
								+ tmpGridPos
								+ " NewGridPos: "
								+ newGridPosition
								+ " exact: "
								+ NumberFormat.getInstance().format(tmp)
								+ "..." /*#Frozen*/
						);
						System.out.println(moved ? "success" /*#Frozen*/
						: "can't move" /*#Frozen*/
						);

					}
				}
			}
		}
		return movements;
	}

	/**@param  toRight <tt>true</tt> = try to move the currentWrapper to right; <tt>false</tt> = try to move the currentWrapper to left;
	 * @param  currentLevel Vector which contains the CellWrappers for the current level
	 * @param  currentIndexInTheLevel
	 * @param  currentPriority
	 * @param  currentWrapper The Wrapper
	 *
	 * @return The free GridPosition or -1 is position is not free.
	 */
	protected boolean move(
		boolean toRight,
		Vector currentLevel,
		int currentIndexInTheLevel,
		int currentPriority) {

		CellWrapper currentWrapper =
			(CellWrapper) currentLevel.get(currentIndexInTheLevel);

		boolean moved = false;
		int neighborIndexInTheLevel =
			currentIndexInTheLevel + (toRight ? 1 : -1);
		int newGridPosition =
			currentWrapper.getGridPosition() + (toRight ? 1 : -1);

		// is the grid position possible?

		if (0 > newGridPosition || newGridPosition >= gridAreaSize) {
			return false;
		}

		// if the node is the first or the last we can move
		if (toRight
			&& currentIndexInTheLevel == currentLevel.size() - 1
			|| !toRight
			&& currentIndexInTheLevel == 0) {

			moved = true;

		} else {
			// else get the neighbor and ask his gridposition
			// if he has the requested new grid position
			// check the priority

			CellWrapper neighborWrapper =
				(CellWrapper) currentLevel.get(neighborIndexInTheLevel);

			int neighborPriority = neighborWrapper.getPriority();

			if (neighborWrapper.getGridPosition() == newGridPosition) {
				if (neighborPriority >= currentPriority) {
					return false;
				} else {
					moved =
						move(
							toRight,
							currentLevel,
							neighborIndexInTheLevel,
							currentPriority);
				}
			} else {
				moved = true;
			}
		}

		if (moved) {
			currentWrapper.setGridPosition(newGridPosition);
		}
		return moved;
	}

	/** This Method draws the graph. For the horizontal position
	 *  we are using the grid position from each graphcell.
	 *  For the vertical position we are using the level position.
	 *
	 */
	protected void drawGraph(
		JGraph jgraph,
		Vector levels,
		Point min,
		Point spacing) {
		// paint the graph

		Map viewMap = new Hashtable();

		for (int rowCellCount = 0;
			rowCellCount < levels.size();
			rowCellCount++) {
			Vector level = (Vector) levels.get(rowCellCount);

			for (int colCellCount = 0;
				colCellCount < level.size();
				colCellCount++) {
				CellWrapper wrapper = (CellWrapper) level.get(colCellCount);
				VertexView view = wrapper.vertexView;

				// remove the temp objects
				/* While the Algorithm is running we are putting some
				 *  attributeNames to the MyGraphCells. This method
				 *  cleans this objects from the MyGraphCells.
				 *
				 */
				view.getAttributes().remove(SUGIYAMA_CELL_WRAPPER);
				view.getAttributes().remove(SUGIYAMA_VISITED);
				wrapper.vertexView = null;

				// get the bounds from the cellView
				if (view == null)
					continue;
				Rectangle bounds = (Rectangle) view.getBounds().clone();

				// adjust
				bounds.x = min.x + spacing.x * wrapper.getGridPosition();
				bounds.y = min.y + spacing.y * rowCellCount;

				Object cell = view.getCell();
				Map map = GraphConstants.createMap();
				GraphConstants.setBounds(map, bounds);

				viewMap.put(cell, map);

			}

		}
		jgraph.getGraphLayoutCache().edit(viewMap, null, null, null);
	}

	/** cell wrapper contains all values
	 *  for one node
	 */
	class CellWrapper implements Comparable {

		/** sum value for edge Crosses
		 */
		private double edgeCrossesIndicator = 0;
		/** counter for additions to the edgeCrossesIndicator
		 */
		private int additions = 0;
		/** the vertical level where the cell wrapper is inserted
		 */
		int level = 0;
		/** current position in the grid
		 */
		int gridPosition = 0;
		/** priority for movements to the barycenter
		 */
		int priority = 0;
		/** reference to the wrapped cell
		 */
		VertexView vertexView = null;

		/** creates an instance and memorizes the parameters
		 *
		 */
		CellWrapper(
			int level,
			double edgeCrossesIndicator,
			VertexView vertexView) {
			this.level = level;
			this.edgeCrossesIndicator = edgeCrossesIndicator;
			this.vertexView = vertexView;
			additions++;
		}

		/** returns the wrapped cell
		 */
		VertexView getVertexView() {
			return vertexView;
		}

		/** resets the indicator for edge crosses to 0
		 */
		void resetEdgeCrossesIndicator() {
			edgeCrossesIndicator = 0;
			additions = 0;
		}

		/** retruns the average value for the edge crosses indicator
		 *
		 *  for the wrapped cell
		 *
		 */

		double getEdgeCrossesIndicator() {
			if (additions == 0)
				return 0;
			return edgeCrossesIndicator / additions;
		}

		/** Addes a value to the edge crosses indicator
		 *  for the wrapped cell
		 *
		 */
		void addToEdgeCrossesIndicator(double addValue) {
			edgeCrossesIndicator += addValue;
			additions++;
		}
		/** gets the level of the wrapped cell
		 */
		int getLevel() {
			return level;
		}

		/** gets the grid position for the wrapped cell
		 */
		int getGridPosition() {
			return gridPosition;
		}

		/** Sets the grid position for the wrapped cell
		 */
		void setGridPosition(int pos) {
			this.gridPosition = pos;
		}

		/** increments the the priority of this cell wrapper.
		 *
		 *  The priority was used by moving the cell to its
		 *  barycenter.
		 *
		 */

		void incrementPriority() {
			priority++;
		}

		/** returns the priority of this cell wrapper.
		 *
		 *  The priority was used by moving the cell to its
		 *  barycenter.
		 */
		int getPriority() {
			return priority;
		}

		/**
		 * @see java.lang.Comparable#compareTo(Object)
		 */
		public int compareTo(Object compare) {
			if (((CellWrapper) compare).getEdgeCrossesIndicator()
				== this.getEdgeCrossesIndicator())
				return 0;

			double compareValue =
				(((CellWrapper) compare).getEdgeCrossesIndicator()
					- this.getEdgeCrossesIndicator());

			return (int) (compareValue * 1000);

		}
	}
}
