/*
 * @(#)ToolsLookAndFeel.java	1.2 02.02.2003
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
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import streamit.eclipse.grapheditor.editor.GPGraphpad;

/**
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class ToolsLookAndFeel extends AbstractActionRadioButton {

	protected UIManager.LookAndFeelInfo[] lookAndFeels = null;
	/**
	 * Constructor for ToolsLookAndFeel.
	 * @param graphpad
	 */
	public ToolsLookAndFeel(GPGraphpad graphpad) {
		super(graphpad);
		lastActionCommand = UIManager.getLookAndFeel().getClass().getName();
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionRadioButton#getPossibleActionCommands()
	 */
	public String[] getPossibleActionCommands() {
		if (lookAndFeels == null)
			lookAndFeels = UIManager.getInstalledLookAndFeels();
		String[] values = new String[lookAndFeels.length];

		for (int i = 0; i < lookAndFeels.length; i++) {
			UIManager.LookAndFeelInfo laf = lookAndFeels[i];
			values[i] = laf.getClassName();
		}
		return values;
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		this.lastActionCommand = e.getActionCommand();
		try {
			UIManager.setLookAndFeel(e.getActionCommand());
			SwingUtilities.updateComponentTreeUI(graphpad.getFrame());
		} catch (Exception ex) {
		};
		update();
	}

	/** updates all Abstract Buttons from this action
	 */
	public void update() {
		Enumeration enum = abstractButtons.elements();
		while (enum.hasMoreElements()) {
			AbstractButton button = (AbstractButton) enum.nextElement();
			button.setSelected(isSelected(button.getActionCommand()));
		}
	};

	/** Should return presentation Text for the 
	 *  action command or null 
	 *  for the default
	 */
	public String getPresentationText(String actionCommand) {
		if (actionCommand == null)
			return null;
			
		for (int i = 0; i < lookAndFeels.length ; i++){
			if (lookAndFeels[i].getClassName().equals(actionCommand)){
				return lookAndFeels[i].getName() ;
			}
		}
		
		return actionCommand;
	}

}
