/*
 * @(#)GraphArrange.java	1.2 29.01.2003
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
import java.util.Map;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import org.jgraph.graph.CellView;
import org.jgraph.graph.GraphConstants;
import streamit.eclipse.grapheditor.editor.pad.Touch;

/**
 * Action enables the function to layout the graph
 * while edit.
 *
 * @author sven.luzar
 *
 */
public class GraphArrange extends AbstractActionDefault {

	/**
	 * Constructor for GraphArrange.
	 * @param graphpad
	 * @param name
	 */
	public GraphArrange(GPGraphpad graphpad) {
		super(graphpad);
	}


	/**
	 * Implementation
	 *
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
		public void actionPerformed(ActionEvent e) {
			Touch touchLayout = graphpad.getCurrentDocument() .getTouch();
			int delay = 250; //milliseconds
			touchLayout.resetDamper();
			touchLayout.start();
			try {
				Thread.sleep(delay);
			} catch (Exception ex) {
				// ignore
			} finally {
				touchLayout.stop();
				CellView[] all =
					graphpad.getCurrentDocument() .getGraphLayoutCache().getAllDescendants(
						graphpad.getCurrentDocument() .getGraphLayoutCache().getRoots());
				Map attributeMap =
					GraphConstants.createAttributes(all, graphpad.getCurrentDocument() .getGraphLayoutCache());
				graphpad.getCurrentDocument() .getGraphLayoutCache().edit(attributeMap, null, null, null);
			}
		}

}
