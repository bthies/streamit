/*
 * @(#)FormatReverse.java	1.2 31.01.2003
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

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.Hashtable;
import java.util.Map;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import org.jgraph.graph.CellView;
import org.jgraph.graph.GraphConstants;

/**
 * Reverse the selected cells
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class FormatReverse extends AbstractActionDefault {

	/**
	 * Constructor for FormatReverse.
	 */
	public FormatReverse(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
			Object[] cells = getCurrentGraph().getSelectionCells();
			if (cells != null) {
				CellView[] views = getCurrentGraphLayoutCache().getMapping(cells);
				Map viewMap = new Hashtable();
				for (int i = 0; i < views.length; i++) {
					Map map = GraphConstants.cloneMap(views[i].getAttributes());
					if (getCurrentGraph().isEdge(views[i].getCell())) {
						int style = GraphConstants.getLineBegin(map);
						int size = GraphConstants.getBeginSize(map);
						boolean fill = GraphConstants.isBeginFill(map);
						GraphConstants.setLineBegin(
							map,
							GraphConstants.getLineEnd(map));
						GraphConstants.setBeginSize(
							map,
							GraphConstants.getEndSize(map));
						GraphConstants.setBeginFill(
							map,
							GraphConstants.isEndFill(map));
						GraphConstants.setLineEnd(map, style);
						GraphConstants.setEndSize(map, size);
						GraphConstants.setEndFill(map, fill);
						viewMap.put(cells[i], map);
					}
					Rectangle bounds = GraphConstants.getBounds(map);
					if (bounds != null) {
						bounds.setSize(bounds.height, bounds.width);
						viewMap.put(views[i].getCell(), map);
					}
				} // for
				getCurrentGraphLayoutCache().edit(viewMap, null, null, null);
			}
	}

}
