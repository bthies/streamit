/*
 * @(#)ToolsShowToolbar.java	1.2 02.02.2003
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
import javax.swing.Icon;

import streamit.eclipse.grapheditor.editor.GPGraphpad;

/**
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class ToolsShowToolbar extends AbstractActionCheckBox {

	/**
	 * Constructor for ToolsShowToolbar.
	 * @param graphpad
	 */
	public ToolsShowToolbar(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Constructor for ToolsShowToolbar.
	 * @param graphpad
	 * @param name
	 */
	public ToolsShowToolbar(GPGraphpad graphpad, String name) {
		super(graphpad, name);
	}

	/**
	 * Constructor for ToolsShowToolbar.
	 * @param graphpad
	 * @param name
	 * @param icon
	 */
	public ToolsShowToolbar(GPGraphpad graphpad, String name, Icon icon) {
		super(graphpad, name, icon);
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionToggle#isSelected(String)
	 */
	public boolean isSelected(String actionCommand) {
		return graphpad.isToolBarsVisible();
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		graphpad.setToolBarsVisible(!graphpad.isToolBarsVisible());
		graphpad.invalidate();
		graphpad.repaint();
		update();
	}
	/** updates all Abstract Buttons from this action
	 */
	public void update(){
		Enumeration enum = abstractButtons.elements();
		while (enum.hasMoreElements()) {
			AbstractButton button = (AbstractButton) enum.nextElement();
			button.setSelected(isSelected(button.getActionCommand()));
		}
	};

}
