/*
 * @(#)WindowCascade.java	1.2 09.02.2003
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
import java.beans.PropertyVetoException;

import javax.swing.JInternalFrame;

import streamit.eclipse.grapheditor.editor.GPGraphpad;

/**
 * Cascades all JInternalFrames at the
 * Desktop.
 *
 * @author sven.luzar
 * @version 1.0
 *
 */
public class WindowCascade extends AbstractActionDefault {

	/**
	 * Constructor for WindowCascade.
	 * @param graphpad
	 */
	public WindowCascade(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Calls the method setLocation, setSize
	 * and toFont for each JInternalFrame.
	 *
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		JInternalFrame[] ajif = graphpad.getAllFrames();
		if (!(ajif.length > 0))
			return;

		try {
			ajif[0].setMaximum(true);
		} catch (PropertyVetoException pve) {
		}

		int desktopX = ajif[0].getX();
		int desktopY = ajif[0].getY();
		int desktopWidth = ajif[0].getWidth();
		int desktopHeight = ajif[0].getHeight();
		int diffWidth = 20;
		int diffHeight = 20;

		for (int i = 0; i < ajif.length; i++) {
			int frmWidth = desktopWidth - (ajif.length - 1) * diffWidth;
			int frmHeight = desktopHeight - (ajif.length - 1) * diffHeight;

			try {
				ajif[i].setIcon(false);
				ajif[i].setMaximum(false);
			} catch (java.beans.PropertyVetoException pvex) {
				// do nothing only display error
			}

			ajif[i].setLocation(desktopX, desktopY);
			ajif[i].setSize(frmWidth, frmHeight);
			ajif[i].toFront();

			try {
				ajif[i].setSelected(true);
			} catch (java.beans.PropertyVetoException pvex) {
				// do nothing only display error
			}

			desktopX += diffWidth;
			desktopY += diffHeight;
		}
	}

}
