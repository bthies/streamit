/*
 * @(#)ShapeGroup.java	1.2 01.02.2003
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

import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.ParentMap;

/**
 * Action that groups the current selection.
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class ShapeGroup extends AbstractActionDefault {

	/**
	 * Constructor for ShapeGroup.
	 * @param graphpad
	 */
	public ShapeGroup(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		Object[] cells = getCurrentGraphLayoutCache().order(getCurrentGraph().getSelectionCells());
		if (cells != null && cells.length > 0) {
			DefaultGraphCell group = new DefaultGraphCell("Group");
			
			// FIX: for the next JGraph binary
		 	ParentMap map = new ParentMap();

			//ParentMap map = new ParentMap();
			for (int i = cells.length-1; i >=0; i--)
				map.addEntry(cells[i], group);
			getCurrentGraph().getGraphLayoutCache().insert(
				new Object[] { group },
				null,
				null,
				map,
				null);
		}
	}

}
