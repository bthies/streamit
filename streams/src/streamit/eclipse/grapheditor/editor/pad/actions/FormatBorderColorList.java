/*
 * @(#)FormatBorderColorList.java	1.2 04.02.2003
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
import java.awt.Rectangle;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.border.Border;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import org.jgraph.graph.CellView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.VertexView;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class FormatBorderColorList extends AbstractActionListCellColor {

	protected Border[] borders =
		new Border[] {
			BorderFactory.createRaisedBevelBorder(),
			BorderFactory.createLoweredBevelBorder(),
			BorderFactory.createEtchedBorder()};

	/**
	 * Constructor for FormatBorderColorList.
	 * @param graphpad
	 */
	public FormatBorderColorList(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionListCell#fillCustomItems(Vector)
	 */
	protected void fillCustomItems(Vector items) {
		VertexView v;
		Map map;

		for (int i = 0; i < borders.length; i++) {
			v =
				new VertexView(
					null,
					dummyGraph,
					dummyGraph.getGraphLayoutCache());
			map = GraphConstants.createMap();
			GraphConstants.setBounds(map, new Rectangle(point, size));
			GraphConstants.setBorder(map, borders[i]);
			GraphConstants.setRemoveAttributes(
				v.getAttributes(),
				new Object[] { GraphConstants.BORDER });
			v.setAttributes(map);
			items.add(v);
		}

		for (int i = 0; i < colors.length; i++) {
			v =
				new VertexView(
					null,
					dummyGraph,
					dummyGraph.getGraphLayoutCache());
			map = GraphConstants.createMap();
			GraphConstants.setBounds(map, new Rectangle(point, size));
			GraphConstants.setBorderColor(map, colors[i]);
			v.setAttributes(map);
			GraphConstants.setRemoveAttributes(
				v.getAttributes(),
				new Object[] { GraphConstants.BORDER });
			items.add(v);
		}
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionListCell#fillResetMap(Map)
	 */
	protected void fillResetMap(Map target) {
		Object[] keys =
			new Object[] { GraphConstants.BORDER, GraphConstants.BORDERCOLOR };
		GraphConstants.setRemoveAttributes(target, keys);
		GraphConstants.setBorderColor(target, Color.BLACK );
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionListCell#fillApplyMap(CellView, Map)
	 */
	protected void fillApplyMap(CellView source, Map target) {
		Object[] keys = new Object[] { GraphConstants.BORDER, GraphConstants.BORDERCOLOR };
		GraphConstants.setRemoveAttributes(target, keys);
		Color c = GraphConstants.getBorderColor(source.getAttributes());
		if (c != null)
			GraphConstants.setBorderColor(target, c );
		Border b = GraphConstants.getBorder(source.getAttributes());
		if (b != null)
			GraphConstants.setBorder(target, b );
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
				Object[] keys = new Object[] { GraphConstants.BORDER };
				GraphConstants.setRemoveAttributes(target, keys);
				GraphConstants.setBorderColor(target, value);
			}
		}
	}

}
