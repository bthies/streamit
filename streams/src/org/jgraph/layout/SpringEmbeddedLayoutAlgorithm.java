/*
 * @(#)SpringEmbeddedLayoutAlgorithm.java	1.0 01/20/03
 *
 * Copyright (C) 2003 Sven Luzar
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
package org.jgraph.layout;

import java.awt.Frame;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.jgraph.JGraph;
import org.jgraph.graph.CellMapper;
import org.jgraph.graph.CellView;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.PortView;
import org.jgraph.graph.VertexView;

/**
 * Arranges the nodes with the Spring Embedded Layout Algorithm.<br>
 *
 * The algorithm takes O(|V|^2 * |E|) time.
 *
 *
 *<br>
 *<br>
 * @author <a href="mailto:Sven.Luzar@web.de">Sven Luzar</a>
 * @since 1.2.2
 * @version 1.0 init
 */
public class SpringEmbeddedLayoutAlgorithm implements LayoutAlgorithm {

	/** Key for an attribute. The value for this key is
	 *  a Rectangle object and specifies the disposement.
	 */
	public static final String SPRING_EMBEDDED_DISP = "SpringEmbeddedDisp";

	/** Key for an attribute. The value for this key is
	 *  a Rectangle object and specifies the calculated position.
	 */
	public static final String SPRING_EMBEDDED_POS = "SpringEmbeddedPos";

	/** Progressbar is shown while the algorithm is running
	 */

	/**
	 * The implementation of the layout algorithm.
	 *
	 * @see LayoutAlgorithm
	 *
	 */
	public void perform(
		JGraph jgraph,
		boolean applyToAll,
		Properties configuration) {

		// memorize the given references
		CellMapper mapper = jgraph.getGraphLayoutCache();

		//---------------------------------------------------------------------------
		// initial work
		//---------------------------------------------------------------------------

		// get the selected cells or all if no cell was selected
		Object[] selectionCells;
		if (applyToAll) {
			selectionCells = jgraph.getRoots();
		} else {
			selectionCells = jgraph.getSelectionCells();
		}
		// create a list with the nodes
		List V =
			new ArrayList(
				(applyToAll
					? jgraph.getModel().getRootCount()
					: selectionCells.length));

		// create a list with the edges
		List E =
			new ArrayList(
				(applyToAll
					? jgraph.getModel().getRootCount()
					: selectionCells.length));

		// Calculate the area
		// (multiply width and length of the frame)
		Rectangle selectionFrame = new Rectangle();
		if (!applyToAll) {
			// variables
			selectionFrame.x = Integer.MAX_VALUE;
			selectionFrame.y = Integer.MAX_VALUE;
			selectionFrame.height = 0;
			selectionFrame.width = 0;
		}
		int maxVertexWidth = 0;

		// Fill the lists with the selected
		// nodes and edges
		for (int i = 0; i < selectionCells.length; i++) {
			CellView cellView = mapper.getMapping(selectionCells[i], false);
			if (cellView instanceof VertexView) {
				V.add(cellView);

				// find the maximum & minimum coordinates
				Rectangle cellViewBounds = cellView.getBounds();

				// checking the selected area
				if (cellViewBounds.x < selectionFrame.x)
					selectionFrame.x = cellViewBounds.x;

				if (cellViewBounds.y < selectionFrame.y)
					selectionFrame.y = cellViewBounds.y;

				int width = cellViewBounds.x - selectionFrame.x;
				if (width > selectionFrame.width)
					selectionFrame.width = width;

				if (cellViewBounds.width > maxVertexWidth) {
					maxVertexWidth = cellViewBounds.width;
				}

				int height = cellViewBounds.y - selectionFrame.y;
				if (height > selectionFrame.height)
					selectionFrame.height = height;
			}
			if (cellView instanceof EdgeView) {
				E.add(cellView);
			}

		}
		if (applyToAll) {
			double boxLength = Math.sqrt(V.size()) * maxVertexWidth;
			selectionFrame =
				new Rectangle(0, 0, (int) boxLength, (int) boxLength);
		}



		// Width of the selectionFrame
		double W = selectionFrame.getWidth();
		// Height of the selectionFrame
		double L = selectionFrame.getHeight();
		// area of the selectionFrame
		double area = W * L;

		// Fill the initial positions with random positions
		Random random = new Random();
		for (int i = 0; i < V.size(); i++) {
			VertexView cellView = (VertexView) V.get(i);
			Rectangle randomPosition =
				new Rectangle(
					random.nextInt(selectionFrame.width),
					random.nextInt(selectionFrame.height),
					cellView.getBounds().width,
					cellView.getBounds().height);

			cellView.getAttributes().put(SPRING_EMBEDDED_POS, randomPosition);
		}

		//---------------------------------------------------------------------------
		// start the iterations
		//---------------------------------------------------------------------------

		// calculate the field length for the area
		double k = Math.sqrt((area) / ((double) V.size()));

		int iterations = 100;
		for (int i = 0; i < iterations; i++) {
			
			/*
			if (dlgProgress.isCanceled()) {

				// clean up the temp objects
				for (int j = 0; j < V.size(); j++) {
					VertexView view = (VertexView) V.get(j);
					view.getAttributes().remove(SPRING_EMBEDDED_POS);
					view.getAttributes().remove(SPRING_EMBEDDED_DISP);
				}

				dlgProgress.setVisible(false);
				return;
			}
			*/

			//---------------------------------------------------------------------------
			// calculate the repulsive forces
			//---------------------------------------------------------------------------

			// calculate the repulsive forces
			for (int vCount = 0; vCount < V.size(); vCount++) {
				VertexView v = (VertexView) V.get(vCount);
				Rectangle vPos =
					(Rectangle) v.getAttributes().get(SPRING_EMBEDDED_POS);

				// each vertex has two vectors: pos and disp
				Rectangle vDisp = new Rectangle(0, 0);
				for (int uCount = 0; uCount < V.size(); uCount++) {
					VertexView u = (VertexView) V.get(uCount);
					if (u != v) {
						// delta is short hand for the difference
						// vector between the positions of the two vertices

						Rectangle uPos =
							(Rectangle) u.getAttributes().get(
								SPRING_EMBEDDED_POS);

						Rectangle delta = new Rectangle();
						delta.x = vPos.x - uPos.x;
						delta.y = vPos.y - uPos.y;

						double fr = fr(norm(delta), k);
						double deltaNormX = delta.x / norm(delta);
						double dispX = deltaNormX * fr;
						double deltaNormY = delta.y / norm(delta);
						double dispY = deltaNormY * fr;

						vDisp.x = vDisp.x + (int) dispX;
						vDisp.y = vDisp.y + (int) dispY;

					}
				}
				v.getAttributes().put(SPRING_EMBEDDED_DISP, vDisp);
			}

			//---------------------------------------------------------------------------
			// calculate the attractive forces
			//---------------------------------------------------------------------------

			for (int cellCount = 0; cellCount < E.size(); cellCount++) {
				//if (E.get(cellCount) instanceof EdgeView) {
				EdgeView e = (EdgeView) E.get(cellCount);
				if (e.getSource() != null
					&& e.getTarget() != null
					&& e.getSource() != e.getTarget()) {

					// extract the used fields
					CellView v =
						(CellView) ((PortView) e.getSource()).getParentView();
					CellView u =
						(CellView) ((PortView) e.getTarget()).getParentView();

					if (v == u)
						continue;

					Rectangle vPos =
						(Rectangle) v.getAttributes().get(SPRING_EMBEDDED_POS);
					Rectangle uPos =
						(Rectangle) u.getAttributes().get(SPRING_EMBEDDED_POS);
					if (vPos == null || uPos == null)
						continue;

					Rectangle vDisp =
						(Rectangle) v.getAttributes().get(SPRING_EMBEDDED_DISP);
					Rectangle uDisp =
						(Rectangle) u.getAttributes().get(SPRING_EMBEDDED_DISP);
					if (vDisp == null || uDisp == null)
						continue;

					// calculate the delta
					Rectangle delta = new Rectangle();
					delta.x = vPos.x - uPos.x;
					delta.y = vPos.y - uPos.y;

					// calculate the attractive forces
					double fa = fa(norm(delta), k);
					double deltaNormX = delta.x / norm(delta);
					double deltaNormY = delta.y / norm(delta);
					double dispX = deltaNormX * fa;
					double dispY = deltaNormY * fa;

					vDisp.x = vDisp.x - (int) dispX;
					vDisp.y = vDisp.y - (int) dispY;
					uDisp.x = uDisp.x + (int) dispX;
					uDisp.y = uDisp.y + (int) dispY;

					// store the new values
					v.getAttributes().put(SPRING_EMBEDDED_DISP, vDisp);
					u.getAttributes().put(SPRING_EMBEDDED_DISP, uDisp);

				}
				//}
			}

			//---------------------------------------------------------------------------
			// calculate the new positions
			//---------------------------------------------------------------------------

			// limit the maximum displacement to the temperature buttonText
			// and then prevent from being displacement outside frame
			double t =
				Math.sqrt(W * W + L * L)
					* ((((double) iterations) / ((double) (i + 1)))
						/ ((double) iterations));

			for (int vCount = 0; vCount < V.size(); vCount++) {
				VertexView v = (VertexView) V.get(vCount);
				Rectangle vDisp =
					((Rectangle) v.getAttributes().get(SPRING_EMBEDDED_DISP));
				Rectangle vPos =
					(Rectangle) v.getAttributes().get(SPRING_EMBEDDED_POS);
				;

				double dispNormX = vDisp.x / norm(vDisp);
				double minX = Math.min(Math.abs(vDisp.x), t);

				double dispNormY = vDisp.y / norm(vDisp);
				double minY = Math.min(Math.abs(vDisp.y), t);

				vPos.x = (int) (vPos.x + dispNormX * minX);
				vPos.y = (int) (vPos.y + dispNormY * minY);

				/*
				double maxX = Math.max(-W / 2, vPos.x);
				double maxY = Math.max(-L / 2, vPos.y);
				double minX2 = Math.min(W / 2, maxX);
				double minY2 = Math.min(L / 2, maxY);
				vPos.x = (int)minX2;
				vPos.y = (int)minY2;
				*/
				v.getAttributes().put(SPRING_EMBEDDED_POS, vPos);

			}

		}



		// find the new positions for the
		// calculated frame
		Rectangle calculatedFrame = new Rectangle();
		for (int vCount = 0; vCount < V.size(); vCount++) {
			VertexView v = (VertexView) V.get(vCount);
			Rectangle vPos =
				(Rectangle) v.getAttributes().get(SPRING_EMBEDDED_POS);

			if (vPos.x < calculatedFrame.x)
				calculatedFrame.x = vPos.x;

			if (vPos.y < calculatedFrame.y)
				calculatedFrame.y = vPos.y;

			int width = vPos.x - calculatedFrame.x;
			if (width > calculatedFrame.width)
				calculatedFrame.width = width;

			int height = vPos.y - calculatedFrame.y;
			if (height > selectionFrame.height)
				calculatedFrame.height = height;

		}

		// calculate the streachfactor and the movement factor
		// to fit the calculated frame to the selected Frame
		double streachX = ((double)selectionFrame.width)  / ((double)calculatedFrame.width) ;
		double streachY = ((double)selectionFrame.width)  / ((double)calculatedFrame.width) ;
		int movementX = selectionFrame.x - calculatedFrame.x;
		int movementY = selectionFrame.y - calculatedFrame.y;

		//---------------------------------------------------------------------------
		// draw the graph
		//---------------------------------------------------------------------------
		Map viewMap = new Hashtable();
		for (int i = 0; i < selectionCells.length; i++) {
			CellView cellView = mapper.getMapping(selectionCells[i], false);
			if (cellView instanceof VertexView) {
				// get the current view object
				VertexView view = (VertexView) cellView;

				// remove the temp objects
				Rectangle newCoord =
					(Rectangle) view.getAttributes().remove(
						SPRING_EMBEDDED_POS);
				view.getAttributes().remove(SPRING_EMBEDDED_DISP);

				// update the location to get the correct
				newCoord.x = (int)((newCoord.x + movementX) * streachX);
				newCoord.y = (int)((newCoord.y + movementY) * streachY);

				// update the view
				Map map = GraphConstants.createMap() ;
				GraphConstants.setBounds(map, newCoord);
				viewMap.put(selectionCells[i], map);

			}
			if (cellView instanceof EdgeView) {
				cellView.update();
			}
		}
		jgraph.getGraphLayoutCache().edit(viewMap, null, null, null);
	}

	/** calculates the attractive forces
	 */
	protected double fa(double x, double k) {
		double force = (x * x / k);
		return force;
	}

	/** calculates the repulsive forces
	 */
	protected double fr(double x, double k) {
		double force = (k * k) / x;
		return force;
	}

	/** Calculates the euklidische Norm
	 *  for the point p.
	 *
	 */
	protected double norm(Rectangle p) {
		double x = p.x;
		double y = p.y;
		double norm = Math.sqrt(x * x + y * y);
		return norm;
	}

}
