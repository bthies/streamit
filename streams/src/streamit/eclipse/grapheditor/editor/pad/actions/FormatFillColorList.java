/*
 * @(#)FormatFillColorList.java	1.2 04.02.2003
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

import javax.swing.Icon;
import javax.swing.JColorChooser;

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
public class FormatFillColorList extends AbstractActionListCellColor {

	/**
	 * Constructor for FormatFillColorList.
	 * @param graphpad
	 */
	public FormatFillColorList(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Constructor for FormatFillColorList.
	 * @param graphpad
	 * @param name
	 */
	public FormatFillColorList(GPGraphpad graphpad, String name) {
		super(graphpad, name);
	}

	/**
	 * Constructor for FormatFillColorList.
	 * @param graphpad
	 * @param name
	 * @param icon
	 */
	public FormatFillColorList(GPGraphpad graphpad, String name, Icon icon) {
		super(graphpad, name, icon);
	}


	protected void fillCustomItems(Vector items){
		VertexView v;
		Map map;

		for (int i = 0; i < colors.length; i++) {
			v =
				new VertexView(
					null,
					dummyGraph,
					dummyGraph.getGraphLayoutCache());
			map = GraphConstants.createMap();
			GraphConstants.setBounds(map, new Rectangle(point, size));
			GraphConstants.setBackground(map, colors[i]);
			GraphConstants.setOpaque(map, true);
			v.setAttributes(map);
			items.add(v);
		}
	}

	protected void fillResetMap(Map target) {
		GraphConstants.setOpaque(target, false);
	}

	protected void fillApplyMap(CellView source, Map target) {
		Color value = GraphConstants.getBackground(source.getAttributes());
		if (value == null)
			return;
		GraphConstants.setOpaque(target, true);
		GraphConstants.setBackground(target, value);
	}

	protected void selectAndFillMap(Map target) {
		Color value =
			JColorChooser.showDialog(
				graphpad.getFrame(),
				Translator.getString("ColorDialog"),
				null);
		if (value != null) {
			GraphConstants.setOpaque(target, true);
			GraphConstants.setBackground(target, value);
		}
	}
}
