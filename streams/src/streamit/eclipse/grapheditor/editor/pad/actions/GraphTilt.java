/*
 * @(#)GraphTilt.java	1.2 01.02.2003
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
import streamit.eclipse.grapheditor.editor.utils.Utilities;

/**
 *
 * @author sven.luzar
 * @version 1.0
 *
 */
public class GraphTilt extends AbstractActionDefault {

	/**
	 * Constructor for GraphTilt.
	 * @param graphpad
	 */
	public GraphTilt(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
			int magnitude = 100;
			Object[] v = getCurrentGraph().getVertices(getCurrentGraph().getAll());
			CellView[] views = getCurrentGraphLayoutCache().getMapping(v);
			if (views != null && views.length > 0) {
				Map attributeMap = new Hashtable();
				for (int i = 0; i < views.length; i++) {
					Rectangle bounds = new Rectangle(views[i].getBounds());
					int dx = Utilities.rnd(magnitude);
					int dy = Utilities.rnd(magnitude);
					int x = Math.max(0, bounds.x + dx - magnitude / 2);
					int y = Math.max(0, bounds.y + dy - magnitude / 2);
					bounds.setLocation(x, y);
					Map attributes = GraphConstants.createMap();
					GraphConstants.setBounds(attributes, bounds);
					attributeMap.put(v[i], attributes);
				}
				getCurrentGraphLayoutCache().edit(attributeMap, null, null, null);
			}
	}

}
