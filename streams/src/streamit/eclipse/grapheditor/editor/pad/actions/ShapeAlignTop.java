/*
 * @(#)ShapeAlignTop.java	1.2 01.02.2003
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
import org.jgraph.graph.GraphLayoutCache;

/**
 * Gets the coordinates (r) for all selected 
 * shapes and aligns the selected 
 * shapes bottommost to the coordinates (r). 
 * 
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class ShapeAlignTop extends AbstractActionDefault {

	/**
	 * Constructor for ShapeAlignTop.
	 * @param graphpad
	 * @param name
	 */
	public ShapeAlignTop(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
			GraphLayoutCache gv = getCurrentGraphLayoutCache();
			Object[] cells = getCurrentGraph().getSelectionCells();
			if (cells != null) {
				Rectangle r = getCurrentGraph().getCellBounds(cells);
				Map viewMap = new Hashtable();
				for (int i = 0; i < cells.length; i++) {
					CellView view = gv.getMapping(cells[i], false);
					Map map = GraphConstants.cloneMap(view.getAllAttributes());
					Rectangle bounds = GraphConstants.getBounds(map);
					if (bounds != null) {
						bounds.setLocation(bounds.x, r.y);
						viewMap.put(cells[i], map);
					}
				}
				gv.edit(viewMap, null, null, null);
			}
	}

}
