/*
 * @(#)FormatTextPositionTop.java	1.2 30.01.2003
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

import javax.swing.JLabel;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import org.jgraph.graph.GraphConstants;

/**
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class FormatTextPositionTop extends AbstractActionDefault {

	/**
	 * Constructor for FormatTextPositionTop.
	 * @param graphpad
	 * @param name
	 */
	public FormatTextPositionTop(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		Map map = GraphConstants.createMap();
		GraphConstants.setVerticalTextPosition(map, JLabel.TOP);
		setSelectionAttributes(map);
	}

}
