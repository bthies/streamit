/*
 * @(#)HelpAbout.java	1.2 01.02.2003
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

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import streamit.eclipse.grapheditor.editor.utils.BrowserLauncher;

/**Shows the homepage.
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class HelpHomepage extends AbstractActionDefault {

	/** The about dialog for JGraphpad
	 */
	protected JDialog aboutDlg;

	/**
	 * Constructor 
	 * @param graphpad
	 */
	public HelpHomepage(GPGraphpad graphpad) {
		super(graphpad);
	}

	/** Opens the url that corresponds to the StreamIt home page</a>.
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		try {		
			BrowserLauncher.openURL("http://cag.lcs.mit.edu/streamit");		
		} catch (Exception ex){
			JOptionPane.showMessageDialog(graphpad, ex.toString(), Translator.getString("Error"), JOptionPane.ERROR_MESSAGE );
		}
		
	}
	/** Empty implementation. 
	 *  This Action should be available
	 *  each time.
	 */
	public void update(){};

}
