/*
 * @(#)ShapeDisconnect.java	1.2 01.02.2003
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
import java.util.HashSet;

import streamit.eclipse.grapheditor.editor.GPGraphpad;

/**
 * Action that disconnects all selected vertices.
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class ShapeDisconnect extends AbstractActionDefault {

	/**
	 * Constructor for ShapeDisconnect.
	 * @param graphpad
	 */
	public ShapeDisconnect(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
			Object[] v = getCurrentGraph().getSelectionVertices();
			if (v != null && v.length > 0) {
				HashSet result = new HashSet();
				for (int i = 0; i < v.length; i++) {
					for (int j = i + 1; j < v.length; j++) {
						Object[] e = getCurrentGraph().getEdgesBetween(v[i], v[j]);
						for (int k = 0; k < e.length; k++)
							result.add(e[k]);
					}
				}
				if (result.size() > 0)
					getCurrentGraph().getModel().remove(result.toArray());
			}
	}

}
