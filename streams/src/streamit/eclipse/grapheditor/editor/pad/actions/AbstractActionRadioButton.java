/*
 * @(#)AbstractActionRadioButton.java	1.2 01.02.2003
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
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPBarFactory;

/**
 *
 * @author sven.luzar
 * @version 1.0
 *
 */
public abstract class AbstractActionRadioButton extends AbstractActionToggle {

	/** Contains the last Action Command
	 *
	 */
	public String lastActionCommand = null;


	/**
	 * Constructor for AbstractActionRadioButton.
	 * @param graphpad
	 */
	public AbstractActionRadioButton(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Constructor for AbstractActionRadioButton.
	 * @param graphpad
	 * @param name
	 */
	public AbstractActionRadioButton(GPGraphpad graphpad, String name) {
		super(graphpad, name);
	}

	/**
	 * Constructor for AbstractActionRadioButton.
	 * @param graphpad
	 * @param name
	 * @param icon
	 */
	public AbstractActionRadioButton(
		GPGraphpad graphpad,
		String name,
		Icon icon) {
		super(graphpad, name, icon);
	}

	public abstract String[] getPossibleActionCommands();

	/**
	 * @see org.jgraph.pad.actions.AbstractActionDefault#getMenuComponents()
	 */
	public Component[] getMenuComponents() {
		String[] actionCommands = getPossibleActionCommands();

		Component[] components = new JComponent[actionCommands.length ];

		for (int i = 0; i < actionCommands.length ; i++){
			components[i] = getMenuComponent(actionCommands[i]);
		}

		return components;
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionDefault#getToolComponents()
	 */
	public Component[] getToolComponents() {
		String[] actionCommands = getPossibleActionCommands();

		Component[] components = new JComponent[actionCommands.length ];

		for (int i = 0; i < actionCommands.length ; i++){
			components[i] = getToolComponent(actionCommands[i]);
		}

		return components;
	}


	/**
	 * @see org.jgraph.pad.actions.AbstractActionDefault#getMenuComponents()
	 */
	protected Component getMenuComponent(String actionCommand) {
		JMenuItem button = new JRadioButtonMenuItem(this);
		GPBarFactory.fillMenuButton(button, getName()+actionCommand, actionCommand);
		abstractButtons.add(button);
		if (lastActionCommand.endsWith(actionCommand))
			button.setSelected(true);
		String presentationText = getPresentationText(actionCommand);
		if (presentationText != null)
			button.setText(presentationText);

		return button;
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionDefault#getToolComponent(String)
	 */
	protected Component getToolComponent(String actionCommand) {
		JRadioButton button = new JRadioButton(this);
		GPBarFactory.fillToolbarButton(button, getName()+actionCommand, actionCommand);
		abstractButtons.add(button);
		if (lastActionCommand.endsWith(actionCommand))
			button.setSelected(true);
		return button;
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionToggle#isSelected(String)
	 */
	public boolean isSelected(String actionCommand) {
		return actionCommand.equals(lastActionCommand);
	}
}
