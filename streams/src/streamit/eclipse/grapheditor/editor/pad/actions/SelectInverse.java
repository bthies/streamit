/*
 * @(#)SelectInverse.java	1.2 01.02.2003
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
import java.util.ArrayList;
import java.util.Set;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import org.jgraph.graph.DefaultGraphModel;

/**
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class SelectInverse extends AbstractActionDefault {

	/**
	 * Constructor for SelectInverse.
	 * @param graphpad
	 */
	public SelectInverse(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		Set set =
			DefaultGraphModel.getDescendants(
				getCurrentGraph().getModel(),
				getCurrentGraph().getSelectionCells());
		ArrayList select = new ArrayList();
		Object[] all = getCurrentGraph().getAll();
		for (int i = 0; i < all.length; i++)
			if (!getCurrentGraph().isGroup(all[i])
				&& !set.contains(all[i])
				&& !getCurrentGraph().isCellSelected(all[i]))
				select.add(all[i]);
		getCurrentGraph().setSelectionCells(select.toArray());
	}

}
