/*
 * @(#)FormatLineColorList.java	1.2 04.02.2003
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

import java.awt.Color;
import java.util.Map;
import java.util.Vector;

import javax.swing.JColorChooser;

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
public class FormatLineColorList extends AbstractActionListCellColor {

	/**
	 * Constructor for FormatLineColorList.
	 * @param graphpad
	 */
	public FormatLineColorList(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionListCell#fillCustomItems(Vector)
	 */
	protected void fillCustomItems(Vector items) {
		for (int i = 0; i < colors.length; i++) {
			EdgeView edge =
				new EdgeView(" ", dummyGraph, dummyGraph.getGraphLayoutCache());
			Map map = GraphConstants.createMap();
			GraphConstants.setPoints(map, arrowPoints);
			GraphConstants.setLineColor(map, colors[i]);
			GraphConstants.setLabelPosition(map, center);
			edge.setAttributes(map);
			items.add(edge);
		}
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionListCell#fillResetMap(Map)
	 */
	protected void fillResetMap(Map target) {
		GraphConstants.setRemoveAttributes(
			target,
			new Object[] { GraphConstants.LINECOLOR });
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionListCell#fillApplyMap(CellView, Map)
	 */
	protected void fillApplyMap(CellView source, Map target) {
		GraphConstants.setLineColor(
			target,
			GraphConstants.getLineColor(source.getAttributes()));
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionListCell#selectAndFillMap(Map)
	 */
	protected void selectAndFillMap(Map target) {
		if (getCurrentGraph().getSelectionCount() > 0) {
			Color value =
				JColorChooser.showDialog(
					graphpad.getFrame(),
					Translator.getString("ColorDialog"),
					null);
			if (value != null) {
				GraphConstants.setLineColor(target, value);
			}
		}
	}

}
