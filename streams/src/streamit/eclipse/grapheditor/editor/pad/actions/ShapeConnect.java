/*
 * @(#)ShapeConnect.java	1.2 01.02.2003
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
import org.jgraph.graph.ConnectionSet;
import streamit.eclipse.grapheditor.editor.pad.GraphModelProvider;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 * Action that connects all selected vertices.
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class ShapeConnect extends AbstractActionDefault {

	/**
	 * Constructor for ShapeConnect.
	 * @param graphpad
	 */
	public ShapeConnect(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		Object[] v = getCurrentGraph().getSelectionVertices();
		if (v != null && v.length < 20) {
			ConnectionSet cs = new ConnectionSet();
			for (int i = 0; i < v.length; i++) {
				for (int j = i + 1; j < v.length; j++) {
					if (!getCurrentGraph().isNeighbour(v[i], v[j])) {

						//Object edge = new DefaultEdge("");
						Object edge =
							getCurrentDocument()
								.getGraphModelProvider()
								.createCell(
								getCurrentGraph().getModel() ,
								GraphModelProvider.CELL_EDGE_DEFAULT,
								"",
								null);
						Object sourcePort =
							getCurrentGraph().getModel().getChild(v[i], 0);
						Object targetPort =
							getCurrentGraph().getModel().getChild(v[j], 0);
						cs.connect(edge, sourcePort, targetPort);
					}
				}
			}
			if (!cs.isEmpty())
				getCurrentGraph().getModel().insert(
					cs.getChangedEdges().toArray(),
					null,
					cs,
					null,
					null);
		} else
			graphpad.error(Translator.getString("TooMany"));
	}

}
