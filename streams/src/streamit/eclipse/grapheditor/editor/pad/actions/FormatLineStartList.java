/*
 * @(#)FormatLineStartList.java	1.2 04.02.2003
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
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.util.Map;
import java.util.Vector;

import javax.swing.JOptionPane;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import org.jgraph.graph.CellView;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 *
 * @author sven.luzar
 * @version 1.0
 *
 */
public class FormatLineStartList extends AbstractActionListCell {

	int[] arrows =
		new int[] {
			GraphConstants.ARROW_CLASSIC,
			GraphConstants.ARROW_TECHNICAL,
			GraphConstants.ARROW_CIRCLE,
			GraphConstants.ARROW_DIAMOND,
			GraphConstants.ARROW_SIMPLE,
			GraphConstants.ARROW_LINE,
			GraphConstants.ARROW_DOUBLELINE };

	/**
	 * Constructor for FormatLineStartList.
	 * @param graphpad
	 */
	public FormatLineStartList(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionListCell#fillCustomItems(Vector)
	 */
	protected void fillCustomItems(Vector items) {

		for (int i = 0; i < arrows.length; i++) {
			EdgeView edge =
				new EdgeView(" ", dummyGraph, dummyGraph.getGraphLayoutCache());
			Map map = GraphConstants.createMap();
			GraphConstants.setPoints(map, this.arrowPoints);
			GraphConstants.setLineBegin(map, arrows[i]);
			GraphConstants.setLabelPosition(map, center);
			edge.setAttributes(map);
			GraphConstants.setRemoveAttributes(
				edge.getAttributes(),
				new Object[] { GraphConstants.BEGINFILL });
			items.add(edge);
		}

		for (int i = 0; i < arrows.length - 3; i++) {
			EdgeView edge =
				new EdgeView(" ", dummyGraph, dummyGraph.getGraphLayoutCache());
			Map map = GraphConstants.createMap();
			GraphConstants.setPoints(map, arrowPoints);
			GraphConstants.setLineBegin(map, arrows[i]);
			GraphConstants.setBeginFill(map, true);
			GraphConstants.setFont(map, GraphConstants.defaultFont.deriveFont(8));
			edge.setAttributes(map);
			items.add(edge);
		}

	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionListCell#fillResetMap(Map)
	 */
	protected void fillResetMap(Map target) {
		Object[] keys = new Object[] { GraphConstants.LINEBEGIN };
		GraphConstants.setRemoveAttributes(target, keys);
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionListCell#fillApplyMap(CellView, Map)
	 */
	protected void fillApplyMap(CellView source, Map target) {
		Object[] keys = new Object[] { GraphConstants.LINEBEGIN, GraphConstants.BEGINFILL
		};
		GraphConstants.setRemoveAttributes(target, keys);
		GraphConstants.setBeginSize (
			target,
			GraphConstants.getBeginSize(source.getAttributes()));
		GraphConstants.setLineBegin(
			target,
			GraphConstants.getLineBegin(source.getAttributes()));

		Boolean fill = (Boolean)source.getAttributes().get(GraphConstants.BEGINFILL);

		if (fill != null){
			GraphConstants.setBeginFill(
				target,
				((Boolean)fill).booleanValue());
		} else {
			GraphConstants.setBeginFill(
				target,
				false);
		}

	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionListCell#selectAndFillMap(Map)
	 */
	protected void selectAndFillMap(Map target) {
		try {
			int s =
				Integer.parseInt(
					JOptionPane.showInputDialog(
						Translator.getString("SizeDialog")));
			Map map = GraphConstants.createMap();
			Object[] keys = new Object[] { GraphConstants.LINEBEGIN };
			GraphConstants.setRemoveAttributes(target, keys);
			GraphConstants.setBeginSize(map, s);
			setSelectionAttributes(map);
		} catch (NullPointerException npe) {
			// ignore
		} catch (Exception ex) {
			graphpad.error(ex.toString());
		}
	}

}
