/*
 * @(#)FormatLinePattern.java	1.2 31.01.2003
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

import javax.swing.JOptionPane;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import org.jgraph.graph.GraphConstants;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import streamit.eclipse.grapheditor.editor.utils.Utilities;

/**
 *
 * @author sven.luzar
 * @version 1.0
 *
 */
public class FormatLinePattern extends AbstractActionDefault {

	/**
	 * Constructor for FormatLinePattern.
	 * @param graphpad
	 * @param name
	 */
	public FormatLinePattern(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (getCurrentGraph().getSelectionCount() > 0) {
			try {
				String s =
					JOptionPane.showInputDialog(
						Translator.getString("PatternDialog"));
				float[] f = Utilities.parsePattern(s);
				if (f != null) {
					Map map = GraphConstants.createMap();
					System.out.println("The pattern selected was "+ f[0] +f[1]);
					GraphConstants.setDashPattern(map, f);
					setSelectionAttributes(map);
				}
			} catch (NullPointerException npe) {
				// ignore
			} catch (Exception ex) {
				graphpad.error(ex.toString());
			}
		}

	}

}
