/*
 * @(#)FormatLineEndList.java	1.2 04.02.2003
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
public class FormatLineEndList extends AbstractActionListCell {

	protected int[] arrows =
		new int[] {
			GraphConstants.ARROW_CLASSIC,
			GraphConstants.ARROW_TECHNICAL,
			GraphConstants.ARROW_CIRCLE,
			GraphConstants.ARROW_DIAMOND,
			GraphConstants.ARROW_SIMPLE,
			GraphConstants.ARROW_LINE,
			GraphConstants.ARROW_DOUBLELINE };

	/**
	 * Constructor for FormatLineEndList.
	 * @param graphpad
	 */
	public FormatLineEndList(GPGraphpad graphpad) {
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
			GraphConstants.setPoints(map, arrowEndPoints);
			GraphConstants.setLineEnd(map, arrows[i]);
			GraphConstants.setLabelPosition(map, center);
			edge.setAttributes(map);
			GraphConstants.setRemoveAttributes(
				edge.getAttributes(),
				new Object[] { GraphConstants.ENDFILL });
			items.add(edge);
		}

		for (int i = 0; i < arrows.length - 3; i++) {
			EdgeView edge =
				new EdgeView(" ", dummyGraph, dummyGraph.getGraphLayoutCache());
			Map map = GraphConstants.createMap();
			GraphConstants.setPoints(map, arrowEndPoints);
			GraphConstants.setLineEnd(map, arrows[i]);
			GraphConstants.setEndFill(map, true);
			GraphConstants.setLabelPosition(map, center);
			edge.setAttributes(map);
			items.add(edge);
		}
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionListCell#fillResetMap(Map)
	 */
	protected void fillResetMap(Map target) {
		Object[] keys = new Object[] { GraphConstants.LINEEND };
		GraphConstants.setRemoveAttributes(target, keys);
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionListCell#fillApplyMap(CellView, Map)
	 */
	protected void fillApplyMap(CellView source, Map target) {
		Object[] keys = new Object[] { GraphConstants.LINEEND };
		GraphConstants.setRemoveAttributes(target, keys);
		GraphConstants.setEndSize(
			target,
			GraphConstants.getEndSize(source.getAttributes()));
		GraphConstants.setLineEnd( 
			target,
			GraphConstants.getLineEnd(source.getAttributes()));
			
		Boolean fill = (Boolean)source.getAttributes().get(GraphConstants.ENDFILL);	
		
		if (fill != null){
			GraphConstants.setEndFill( 
				target,
				((Boolean)fill).booleanValue());
		} else {
			GraphConstants.setEndFill( 
				target,
				false);
		}
			
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionListCell#selectAndFillMap(Map)
	 */
	protected void selectAndFillMap(Map target) {
		if (getCurrentGraph().getSelectionCount() > 0) {
			try {
				int s =
					Integer.parseInt(
						JOptionPane.showInputDialog(
							Translator.getString("SizeDialog")));
				GraphConstants.setEndSize(target, s);
			} catch (NullPointerException npe) {
				// ignore
			} catch (Exception ex) {
				graphpad.error(ex.toString());
			}
		}
	}

}
