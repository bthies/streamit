/*
 * @(#)FilePrint.java	1.2 30.01.2003
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
import java.awt.print.PrinterJob;

import streamit.eclipse.grapheditor.editor.GPGraphpad;

/**
 * Prints the current graph.
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class FilePrint extends AbstractActionDefault {

	/**
	 * Constructor for FilePrint.
	 * @param graphpad
	 * @param name
	 */
	public FilePrint(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
			PrinterJob printJob = PrinterJob.getPrinterJob();
			printJob.setPrintable(graphpad.getCurrentDocument() );
			if (printJob.printDialog()) {
				try {
					boolean oldvalue = false;
					double oldscale = getCurrentGraph().getScale();
					graphpad.getCurrentDocument().setScale(1);
					oldvalue = getCurrentGraph().isPageVisible();
					printJob.print();
					graphpad.getCurrentDocument() .setScale(oldscale);
					getCurrentGraph().setPageVisible(oldvalue);
				} catch (Exception printException) {
					printException.printStackTrace();
				}
			}
	}

}
