/*
 * @(#)AbstractActionCheckBox.java	1.2 01.02.2003
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

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPBarFactory;

/**
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public abstract class AbstractActionCheckBox extends AbstractActionToggle {

	/**
	 * Constructor for AbstractActionCheckBox.
	 * @param graphpad
	 */
	public AbstractActionCheckBox(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Constructor for AbstractActionCheckBox.
	 * @param graphpad
	 * @param name
	 */
	public AbstractActionCheckBox(GPGraphpad graphpad, String name) {
		super(graphpad, name);
	}

	/**
	 * Constructor for AbstractActionCheckBox.
	 * @param graphpad
	 * @param name
	 * @param icon
	 */
	public AbstractActionCheckBox(
		GPGraphpad graphpad,
		String name,
		Icon icon) {
		super(graphpad, name, icon);
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionDefault#getMenuComponent(String)
	 */
	protected Component getMenuComponent(String actionCommand) {
		JCheckBoxMenuItem button = new JCheckBoxMenuItem(this);
		abstractButtons.add(button);
		GPBarFactory.fillMenuButton(button, getName(), actionCommand);
		String presentationText = getPresentationText(actionCommand);
		if (presentationText != null)
			button.setText(presentationText);
		
		return button;
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionDefault#getToolComponent(String)
	 */
	protected Component getToolComponent(String actionCommand) {
		JCheckBox button = new JCheckBox(this);
		abstractButtons.add(button);
		GPBarFactory.fillToolbarButton(button, getName(), actionCommand);
		return button;
	}
}
