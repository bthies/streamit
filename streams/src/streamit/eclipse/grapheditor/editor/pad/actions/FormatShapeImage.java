/*
 * @(#)FormatShapeImage.java	1.2 29.01.2003
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

import javax.swing.ImageIcon;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import org.jgraph.graph.GraphConstants;
import streamit.eclipse.grapheditor.editor.pad.ImageIconBean;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 * Open a dialog to load an image of the selected cells
 *
 * @author sven.luzar
 */
public class FormatShapeImage extends AbstractActionFile {

	/**
	 * Constructor for FormatShapeImage.
	 * @param graphpad
	 * @param name
	 */
	public FormatShapeImage(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e){
			// Should also offer a possibility to enter URL here
	  		String name = openDialog(Translator.getString("ImageDialog"), null, null);
			if (name != null) {
				try {
					//File f = new File(name);
					//ImageIcon icon = new ImageIcon(ImageIO.read(f));
					// JDK 1.3
					System.out.println("THE NAME OF THE IMAGE IS "+ name);
					ImageIcon icon = new ImageIconBean(name);
					Map map = GraphConstants.createMap();
					GraphConstants.setIcon(map, icon);
					graphpad.getCurrentDocument() .setSelectionAttributes(map);
				} catch (Exception ex) {
					graphpad.error(ex.toString());
					ex.printStackTrace();
				}
			}
	}

}
